package com.example.data

object UnityVrExportHelper {
    fun buildUnityVrExportBundle(): String = """
// ============================================================================
// DuelPintar3D VR — Paket Skrip Unity 3D VR (Ruang Kelas & Halaman Sekolah + Timer)
// Panduan Import ke Unity Editor (2022.3 LTS / Unity 6):
// 1. Buat proyek Unity 3D (Built-in / URP) dengan target platform Android.
// 2. Aktifkan XR Plug-in Management -> Google Cardboard XR Plugin / OpenXR.
// 3. Buat 1 Empty GameObject bernama "DuelPintarVR", tempelkan skrip
//    DuelPintarVrManager & DuelPintarVrSceneBuilder, lalu tekan Play / Build APK!
// ============================================================================

using UnityEngine;
using UnityEngine.UI;
using System.Collections.Generic;

namespace DuelPintar3D.VR
{
    public enum VrSchoolSceneMode { RuangKelas3D, HalamanSekolahLuas }

    [System.Serializable]
    public class QuizQuestion3D
    {
        public string subject;
        public string prompt;
        public string[] options = new string[4];
        public int correctIndex;
        public string explanation;
    }

    public class VrAnswerBlock : MonoBehaviour
    {
        public int blockIndex;
        private DuelPintarVrManager manager;
        private Renderer cubeRenderer;
        private TextMesh labelMesh;
        private Color baseColor;

        public void InitVisual(Color color)
        {
            baseColor = color;
            cubeRenderer = GetComponent<Renderer>();
            if (cubeRenderer != null) cubeRenderer.material.color = color;

            GameObject textObj = new GameObject("Label");
            textObj.transform.SetParent(transform, false);
            textObj.transform.localPosition = new Vector3(0f, 0f, -0.6f);
            labelMesh = textObj.AddComponent<TextMesh>();
            labelMesh.alignment = TextAlignment.Center;
            labelMesh.anchor = TextAnchor.MiddleCenter;
            labelMesh.characterSize = 0.14f;
            labelMesh.fontSize = 36;
            labelMesh.color = Color.white;
        }

        public void Setup(int index, string optionText, DuelPintarVrManager owner)
        {
            blockIndex = index;
            manager = owner;
            string prefix = index switch { 0 => "A", 1 => "B", 2 => "C", _ => "D" };
            if (labelMesh != null) labelMesh.text = prefix + "\n" + optionText;
            if (cubeRenderer != null) cubeRenderer.material.color = baseColor;
        }

        public void TriggerSelect()
        {
            if (manager != null) manager.OnBlockSelected(blockIndex);
        }

        void OnMouseDown()
        {
            TriggerSelect();
        }
    }

    public class DuelPintarVrManager : MonoBehaviour
    {
        [Header("Lingkungan Sekolah 3D VR")]
        public GameObject classroomRoot;
        public GameObject schoolyardRoot;
        public VrSchoolSceneMode activeSceneMode = VrSchoolSceneMode.RuangKelas3D;

        [Header("Pengaturan Timer Kuis")]
        public float quizDurationSeconds = 120f;
        private float timeRemaining;
        private bool quizEnded = false;
        public Text timerText3D;

        [Header("Arena Blok 3D")]
        public Transform centerSubjectCube;
        public VrAnswerBlock[] answerBlocks = new VrAnswerBlock[4];
        public Text questionText3D;
        public TextMesh worldQuestionMesh;
        public TextMesh worldTimerMesh;
        public AudioSource audioSource;
        public AudioClip correctSfx;
        public AudioClip wrongSfx;
        public List<QuizQuestion3D> questions = new List<QuizQuestion3D>();
        private int currentIndex = 0;
        private int score = 0;

        void Awake()
        {
            if (classroomRoot == null || schoolyardRoot == null)
            {
                BuildDefaultVrWorld();
            }
            if (questions.Count == 0)
            {
                questions.Add(new QuizQuestion3D
                {
                    subject = "Matematika",
                    prompt = "Berapa hasil 24 + 18?",
                    options = new[] { "32", "42", "44", "52" },
                    correctIndex = 1,
                    explanation = "24 + 18 = 42"
                });
            }
        }

        void Start()
        {
            timeRemaining = quizDurationSeconds;
            SwitchEnvironment(activeSceneMode);
            LoadCurrentQuestion();
        }

        void Update()
        {
            if (quizEnded) return;

            timeRemaining -= Time.deltaTime;
            int sec = Mathf.Max(0, Mathf.CeilToInt(timeRemaining));
            string formatted = $"{sec / 60:00}:{sec % 60:00}";
            if (timerText3D != null) timerText3D.text = formatted;
            if (worldTimerMesh != null) worldTimerMesh.text = "Waktu: " + formatted;

            if (timeRemaining <= 0f)
            {
                EndQuizByTimeout();
                return;
            }

            if (centerSubjectCube != null)
            {
                centerSubjectCube.Rotate(Vector3.up * 35f * Time.deltaTime);
            }
        }

        public void SwitchEnvironment(VrSchoolSceneMode mode)
        {
            activeSceneMode = mode;
            if (classroomRoot != null) classroomRoot.SetActive(mode == VrSchoolSceneMode.RuangKelas3D);
            if (schoolyardRoot != null) schoolyardRoot.SetActive(mode == VrSchoolSceneMode.HalamanSekolahLuas);
        }

        public void EndQuizByTimeout()
        {
            quizEnded = true;
            string msg = $"Waktu Habis! Kuis Berakhir • Skor: {score}";
            if (questionText3D != null) questionText3D.text = msg;
            if (worldQuestionMesh != null) worldQuestionMesh.text = msg;
        }

        public void LoadCurrentQuestion()
        {
            if (quizEnded || currentIndex >= questions.Count) return;
            var q = questions[currentIndex];
            if (questionText3D != null) questionText3D.text = q.prompt;
            if (worldQuestionMesh != null) worldQuestionMesh.text = q.prompt;
            for (int i = 0; i < answerBlocks.Length && i < 4; i++)
            {
                if (answerBlocks[i] != null) answerBlocks[i].Setup(i, q.options[i], this);
            }
        }

        public void OnBlockSelected(int chosenIndex)
        {
            if (quizEnded) return;
            var q = questions[currentIndex];
            if (chosenIndex == q.correctIndex)
            {
                score += 120;
                if (audioSource != null && correctSfx != null) audioSource.PlayOneShot(correctSfx);
            }
            else
            {
                if (audioSource != null && wrongSfx != null) audioSource.PlayOneShot(wrongSfx);
            }
            currentIndex++;
            if (currentIndex < questions.Count) Invoke(nameof(LoadCurrentQuestion), 1.2f);
            else
            {
                quizEnded = true;
                if (worldQuestionMesh != null) worldQuestionMesh.text = $"Selesai! Skor Akhir: {score}";
            }
        }

        private void BuildDefaultVrWorld()
        {
            classroomRoot = new GameObject("RuangKelas3D");
            CreateBox(classroomRoot.transform, "LantaiKelas", new Vector3(0, -1.6f, 0), new Vector3(12f, 0.2f, 12f), new Color(0.88f, 0.78f, 0.62f));
            CreateBox(classroomRoot.transform, "DindingBelakang", new Vector3(0, 1.5f, 5.5f), new Vector3(12f, 6.2f, 0.3f), new Color(0.95f, 0.92f, 0.84f));
            CreateBox(classroomRoot.transform, "PapanTulisHijau", new Vector3(0, 1.4f, 5.2f), new Vector3(5.2f, 2.4f, 0.1f), new Color(0.10f, 0.38f, 0.23f));
            CreateBox(classroomRoot.transform, "MejaGuru", new Vector3(0, -0.9f, 3.3f), new Vector3(2.2f, 0.8f, 1.0f), new Color(0.54f, 0.34f, 0.18f));

            schoolyardRoot = new GameObject("HalamanSekolahLuas");
            CreateBox(schoolyardRoot.transform, "LapanganRumput", new Vector3(0, -1.6f, 0), new Vector3(24f, 0.2f, 24f), new Color(0.24f, 0.65f, 0.32f));
            CreateBox(schoolyardRoot.transform, "GedungSekolah", new Vector3(0, 1.4f, 8.5f), new Vector3(12f, 5.8f, 2.5f), new Color(0.96f, 0.90f, 0.75f));
            CreateBox(schoolyardRoot.transform, "AtapSekolah", new Vector3(0, 4.6f, 8.5f), new Vector3(13f, 0.9f, 3.1f), new Color(0.75f, 0.22f, 0.18f));
            CreateBox(schoolyardRoot.transform, "TiangBendera", new Vector3(-4.5f, 1.5f, 4.5f), new Vector3(0.15f, 6.2f, 0.15f), Color.white);
            CreateBox(schoolyardRoot.transform, "BenderaMerah", new Vector3(-3.85f, 4.15f, 4.5f), new Vector3(1.2f, 0.4f, 0.05f), Color.red);
            CreateBox(schoolyardRoot.transform, "BenderaPutih", new Vector3(-3.85f, 3.75f, 4.5f), new Vector3(1.2f, 0.4f, 0.05f), Color.white);

            GameObject centerObj = GameObject.CreatePrimitive(PrimitiveType.Cube);
            centerObj.name = "CenterSubjectCube";
            centerObj.transform.position = new Vector3(0, 0.6f, 3.0f);
            centerObj.GetComponent<Renderer>().material.color = new Color(0.15f, 0.55f, 0.95f);
            centerSubjectCube = centerObj.transform;

            Color[] colors = { new Color(0.23f, 0.51f, 0.96f), new Color(0.06f, 0.73f, 0.51f), new Color(0.96f, 0.62f, 0.04f), new Color(0.93f, 0.28f, 0.60f) };
            for (int i = 0; i < 4; i++)
            {
                GameObject blockObj = GameObject.CreatePrimitive(PrimitiveType.Cube);
                blockObj.name = "AnswerBlock_" + i;
                blockObj.transform.position = new Vector3(-3.0f + i * 2.0f, -0.35f, 2.2f);
                blockObj.transform.localScale = new Vector3(1.25f, 0.95f, 0.95f);
                VrAnswerBlock block = blockObj.AddComponent<VrAnswerBlock>();
                block.InitVisual(colors[i]);
                answerBlocks[i] = block;
            }

            GameObject qObj = new GameObject("WorldQuestionText");
            qObj.transform.position = new Vector3(0, 2.3f, 3.2f);
            worldQuestionMesh = qObj.AddComponent<TextMesh>();
            worldQuestionMesh.alignment = TextAlignment.Center;
            worldQuestionMesh.anchor = TextAnchor.MiddleCenter;
            worldQuestionMesh.characterSize = 0.12f;
            worldQuestionMesh.fontSize = 36;

            GameObject tObj = new GameObject("WorldTimerText");
            tObj.transform.position = new Vector3(0, 3.0f, 3.2f);
            worldTimerMesh = tObj.AddComponent<TextMesh>();
            worldTimerMesh.alignment = TextAlignment.Center;
            worldTimerMesh.anchor = TextAnchor.MiddleCenter;
            worldTimerMesh.characterSize = 0.12f;
            worldTimerMesh.fontSize = 34;
            worldTimerMesh.color = Color.yellow;
        }

        private void CreateBox(Transform parent, string objName, Vector3 pos, Vector3 scale, Color color)
        {
            GameObject box = GameObject.CreatePrimitive(PrimitiveType.Cube);
            box.name = objName;
            box.transform.SetParent(parent, false);
            box.transform.position = pos;
            box.transform.localScale = scale;
            box.GetComponent<Renderer>().material.color = color;
        }
    }
}
    """.trimIndent()
}
