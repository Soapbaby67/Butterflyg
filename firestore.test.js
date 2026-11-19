const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validCampaignPayload(userId) {
  return {
    userId,
    title: "Тень Конохи",
    universe: "Наруто",
    characterSummary: "Попаданец Кайто, скрытые печати",
    startingPoint: "Перед экзаменом на чунина",
    toneAndDifficulty: "Мрачный реализм",
    turnCount: 1,
    hudLocation: "Ворота Конохи",
    hudTime: "Рассвет",
    hudStatusAndInventory: "Здоров | Кунай, свиток",
    hudRelationsAndCanon: "Ирука: доверие | Канон: 5%",
    canonDeviationPercent: 5,
    activeNpc: {
      hasActiveNpc: true,
      name: "Ирука Умино",
      emotionLabel: "Сдержанная теплота",
      emotionCategory: "calm",
      intensity: 2,
      note: "Внимательно вглядывается в незнакомца.",
    },
    inventory: ["Кунай", "Свиток"],
    inventoryItems: [
      {
        id: "kunai_1",
        name: "Кунай",
        category: "weapon",
        quantity: 2,
        isEquipped: true,
        description: "Стандартный клинок шиноби",
      },
    ],
    playerName: "Кайто",
    playerGender: "male",
    healthPercent: 100,
    currencyAmount: 50,
    currencyLabel: "рё",
    skills: { might: 5, stealth: 7, insight: 6, lore: 5 },
    chapterSummaries: [],
    quests: [{ id: "q1", title: "Пройти экзамен", status: "active", updatedAtTurn: 1 }],
    activeQuests: ["Пройти экзамен"],
    npcs: [
      {
        name: "Ирука Умино",
        role: "Наставник",
        trustLevel: 15,
        statusLabel: "Доброжелателен",
        lastEmotion: "Сдержанная теплота",
        lastEmotionCategory: "calm",
        recentActions: ["Показал пропуск"],
      },
    ],
    butterflyEvents: [],
    storySummary: "Прибытие в Коноху.",
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

function validTurnPayload(userId, campaignId) {
  return {
    userId,
    campaignId,
    turnNumber: 1,
    playerAction: "Начало хроники",
    narrative: "Утренний туман стелется над крышами Конохи...",
    hudLine: "[Ворота Конохи | Рассвет | Здоров | Канон: 5%]",
    activeNpc: {
      hasActiveNpc: true,
      name: "Ирука Умино",
      emotionLabel: "Любопытство",
      emotionCategory: "surprise",
      intensity: 2,
      note: "Приподнимает бровь.",
    },
    choices: [
      { id: "1", approach: "Дипломатический", text: "Представиться и показать пропуск" },
      { id: "2", approach: "Скрытный", text: "Обойти пост по крышам" },
    ],
    stateSnapshot: {
      hudLocation: "Ворота Конохи",
      hudTime: "Рассвет",
      hudStatusAndInventory: "Здоров | Кунай, свиток",
      hudRelationsAndCanon: "Ирука: доверие | Канон: 5%",
      canonDeviationPercent: 5,
      healthPercent: 100,
      currencyAmount: 50,
      currencyLabel: "рё",
    },
    createdAt: new Date(),
  };
}

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

test("Unauthenticated user cannot read or list campaigns", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/campaigns`).get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/campaigns/camp_1`).get());
});

test("Authenticated owner can create, read, update, and delete own campaign", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const campRef = aliceDb.doc(`users/${ALICE_UID}/campaigns/camp_1`);

  await assertSucceeds(campRef.set(validCampaignPayload(ALICE_UID)));
  await assertSucceeds(campRef.get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/campaigns`).get());
  await assertSucceeds(
    campRef.update({
      turnCount: 2,
      canonDeviationPercent: 18,
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(campRef.delete());
});

test("Cross-user access to campaigns and turns is denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.doc(`users/${ALICE_UID}/campaigns/camp_1`).set(validCampaignPayload(ALICE_UID));
    await adminDb.doc(`users/${ALICE_UID}/campaigns/camp_1/turns/turn_1`).set(validTurnPayload(ALICE_UID, "camp_1"));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/campaigns`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/campaigns/camp_1`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/campaigns/camp_1`).update({ title: "Hacked" }));
  await assertFails(bobDb.doc(`users/${ALICE_UID}/campaigns/camp_1/turns/turn_1`).get());
});

test("Shadow update with ghost field or userId spoofing fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const campRef = aliceDb.doc(`users/${ALICE_UID}/campaigns/camp_1`);

  const spoofed = validCampaignPayload(BOB_UID);
  await assertFails(campRef.set(spoofed));

  const ghost = { ...validCampaignPayload(ALICE_UID), isAdmin: true };
  await assertFails(campRef.set(ghost));

  await assertSucceeds(campRef.set(validCampaignPayload(ALICE_UID)));
  await assertFails(campRef.update({ isAdmin: true, updatedAt: new Date() }));
});

test("Owner can create and read turns only for existing campaign; updates to turns fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const orphanTurnRef = aliceDb.doc(`users/${ALICE_UID}/campaigns/missing_camp/turns/turn_1`);
  await assertFails(orphanTurnRef.set(validTurnPayload(ALICE_UID, "missing_camp")));

  const campRef = aliceDb.doc(`users/${ALICE_UID}/campaigns/camp_1`);
  await assertSucceeds(campRef.set(validCampaignPayload(ALICE_UID)));

  const turnRef = aliceDb.doc(`users/${ALICE_UID}/campaigns/camp_1/turns/turn_1`);
  await assertSucceeds(turnRef.set(validTurnPayload(ALICE_UID, "camp_1")));
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/campaigns/camp_1/turns`).get());
  await assertFails(turnRef.update({ narrative: "Edited" }));
});
