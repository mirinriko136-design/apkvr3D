const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_student_1";
const BOB_UID = "bob_student_2";
const ADMIN_EMAIL = "mirinriko136@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function nowPast() {
  return new Date(Date.now() - 2000);
}

test("Unauthenticated users are denied all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("leaderboard").where("totalXp", ">=", 0).get());
  await assertFails(unauthDb.collection("questions").where("published", "==", true).get());
  await assertFails(unauthDb.collection("duel_rooms").where("status", "==", "WAITING").get());
});

test("User profile: owner create, partial update, cross-user & ghost-field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = nowPast();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Budi Pintar",
      role: "student",
      gradeLevel: 4,
      schoolName: "SDN Nusantara 01",
      avatarColor: "#2563EB",
      totalXp: 120,
      level: 2,
      streakDays: 3,
      totalCorrect: 10,
      totalAnswered: 12,
      duelWins: 1,
      duelPlayed: 2,
      soundEnabled: true,
      biometricEnabled: false,
      themeMode: "system",
      dailyReminderEnabled: true,
      dailyReminderHour: 18,
      dailyReminderMinute: 30,
      dashboardWidgets: ["vr_arena", "multiplayer", "weekly_report"],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update preserving createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      totalXp: 240,
      level: 3,
      themeMode: "dark",
      updatedAt: nowPast(),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: nowPast(),
    })
  );

  // Reject cross-user read/write
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: nowPast(),
    })
  );
});

test("Quiz sessions & Study schedules subcollections: owner scoped CRUD", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = nowPast();

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("quiz_sessions")
      .doc("sess_1")
      .set({
        userId: ALICE_UID,
        subject: "MATEMATIKA",
        mode: "SOLO_VR",
        score: 450,
        correctCount: 5,
        totalQuestions: 5,
        durationSeconds: 75,
        xpEarned: 90,
        dayKey: "2026-11-24",
        createdAt: created,
      })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("quiz_sessions").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("quiz_sessions").get()
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("study_schedules")
      .doc("sched_1")
      .set({
        userId: ALICE_UID,
        title: "Latihan Pecahan IPA",
        subject: "MATEMATIKA",
        dueAtMillis: Date.now() + 3600000,
        reminderMinutesBefore: 30,
        completed: false,
        calendarSynced: true,
        notes: "Bab 4 kelas 5 SD",
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("study_schedules")
      .doc("sched_1")
      .update({
        completed: true,
        updatedAt: nowPast(),
      })
  );
});

test("Leaderboard: public query alignment & owner-only write", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = nowPast();

  await assertSucceeds(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Budi Pintar",
      schoolName: "SDN 01",
      gradeLevel: 4,
      totalXp: 350,
      level: 3,
      duelWins: 2,
      accuracyPct: 88,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Aligned query succeeds; unfiltered query fails
  await assertSucceeds(
    bobDb.collection("leaderboard").where("totalXp", ">=", 0).get()
  );
  await assertFails(bobDb.collection("leaderboard").get());

  // Cross-user write fails
  await assertFails(
    bobDb.collection("leaderboard").doc(ALICE_UID).update({
      totalXp: 99999,
      updatedAt: nowPast(),
    })
  );
});

test("Questions bank: teacher author CRUD, published query, admin verified vs spoofed email", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv
    .authenticatedContext("admin_uid", { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const spoofAdminDb = testEnv
    .authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();
  const created = nowPast();

  await assertSucceeds(
    aliceDb.collection("questions").doc("q_1").set({
      authorId: ALICE_UID,
      authorName: "Ibu Sari",
      subject: "IPA",
      gradeLevel: 5,
      difficulty: "MUDAH",
      prompt: "Planet terdekat dari Matahari adalah?",
      options: ["Merkurius", "Venus", "Bumi", "Mars"],
      correctIndex: 0,
      explanation: "Merkurius berada paling dekat dengan Matahari.",
      published: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Published query succeeds
  await assertSucceeds(
    bobDb.collection("questions").where("published", "==", true).get()
  );
  // Unfiltered list fails for non-admin
  await assertFails(bobDb.collection("questions").get());
  // Non-author non-admin cannot edit
  await assertFails(
    bobDb.collection("questions").doc("q_1").update({
      prompt: "Diubah sembarangan?",
      updatedAt: nowPast(),
    })
  );
  // Spoofed unverified admin email cannot edit
  await assertFails(
    spoofAdminDb.collection("questions").doc("q_1").update({
      prompt: "Spoofed update?",
      updatedAt: nowPast(),
    })
  );
  // Verified admin can moderate/update
  await assertSucceeds(
    adminDb.collection("questions").doc("q_1").update({
      difficulty: "SEDANG",
      updatedAt: nowPast(),
    })
  );
});

test("Duel rooms: create, guest join, progress updates, terminal lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = nowPast();

  await assertSucceeds(
    aliceDb.collection("duel_rooms").doc("room_abc123").set({
      roomCode: "ABC123",
      subject: "BAHASA_INDONESIA",
      gradeLevel: 4,
      status: "WAITING",
      hostId: ALICE_UID,
      hostName: "Budi",
      hostScore: 0,
      hostProgress: 0,
      guestScore: 0,
      guestProgress: 0,
      questionSeeds: ["0", "1", "2", "3", "4"],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Browse waiting rooms
  await assertSucceeds(
    bobDb.collection("duel_rooms").where("status", "==", "WAITING").get()
  );

  // Bob joins room
  await assertSucceeds(
    bobDb.collection("duel_rooms").doc("room_abc123").update({
      guestId: BOB_UID,
      guestName: "Siti",
      status: "PLAYING",
      updatedAt: nowPast(),
    })
  );

  // Host updates score & finishes room
  await assertSucceeds(
    aliceDb.collection("duel_rooms").doc("room_abc123").update({
      hostScore: 300,
      hostProgress: 5,
      status: "FINISHED",
      winnerId: ALICE_UID,
      updatedAt: nowPast(),
    })
  );

  // Terminal state lock rejects further updates
  await assertFails(
    bobDb.collection("duel_rooms").doc("room_abc123").update({
      guestScore: 999,
      updatedAt: nowPast(),
    })
  );
});
