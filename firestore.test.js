const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_architect_1";
const BOB_UID = "bob_architect_2";
const GUEST_UID = "guest_studio_architect_demo";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validKitchenPayload(userId, now) {
  return {
    userId,
    name: "مطبخ فيلا الياسمين الحديث",
    shape: "ISLAND",
    styleId: "MODERN_WALNUT",
    roomWidthCm: 420,
    roomDepthCm: 360,
    roomHeightCm: 280,
    cabinetFinish: "جوز أمريكي طبيعي",
    countertopMaterial: "رخام كالكاتا إيطالي",
    handleStyle: "نحاس مصقول",
    wallColorHex: "#F5F2EB",
    floorMaterial: "باركيه بلوط دافئ",
    itemsData: "#VIEW3D|38.0|0.54|1.0|ISOMETRIC_RIGHT|DAYLIGHT;SINK|120|0|90|60|0|حوض غسيل مزدوج|2400;STOVE|240|0|90|60|0|فرن وموقد بلت إن|4200;FRIDGE|0|120|90|70|90|ثلاجة مدمجة ذكية|5800",
    totalPriceSar: 28400,
    ergonomicScore: 94,
    notes: "تصميم مع جزيرة وسطية وإضاءة مخفية",
    createdAt: now,
    updatedAt: now,
  };
}

function validQuotePayload(userId, projectId, now) {
  return {
    userId,
    projectId,
    projectName: "مطبخ فيلا الياسمين الحديث",
    showroomName: "دار المطابخ الألمانية - الرياض",
    customerName: "عبدالله الفيصل",
    customerPhone: "+966501234567",
    city: "الرياض",
    preferredInstallWindow: "خلال 30 يوم",
    estimatedBudgetSar: 28400,
    specialNotes: "يرجى التواصل لتحديد موعد رفع المقاسات بالليزر",
    status: "pending",
    createdAt: now,
    updatedAt: now,
  };
}

function validCatalogUnitPayload(userId, now) {
  return {
    userId,
    code: "FRIDGE",
    nameAr: "ثلاجة وفريزر مدمج دولابي الذكي",
    category: "APPLIANCE",
    subType: "FRIDGE",
    widthCm: 90,
    depthCm: 70,
    heightCm: 215,
    elevationCm: 0,
    priceSar: 6200,
    colorHex: 0xff3e6b89,
    materialFinish: "ستانلس ستيل مضاد للبصمات",
    countertopIncluded: false,
    descriptionAr: "ثلاجة دولابية مدمجة بسعة 540 لتر مع تبريد مزدوج",
    createdAt: now,
    updatedAt: now,
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

test("Unauthenticated user: cannot read or create arbitrary user kitchen_projects or kitchen_units_catalog", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("kitchen_projects").get());
  await assertFails(unauthDb.collection("kitchen_units_catalog").get());
  await assertFails(
    unauthDb
      .collection("kitchen_projects")
      .doc("proj_1")
      .set(validKitchenPayload(ALICE_UID, new Date()))
  );
  await assertFails(
    unauthDb
      .collection("kitchen_units_catalog")
      .doc("unit_1")
      .set(validCatalogUnitPayload(ALICE_UID, new Date()))
  );
});

test("Guest Studio Bypass user: can save, retrieve, and modify their own guest_studio_ 3D kitchen projects", async () => {
  const guestDb = testEnv.unauthenticatedContext().firestore();
  const createdAt = new Date(Date.now() - 5000);
  const payload = validKitchenPayload(GUEST_UID, createdAt);

  await assertSucceeds(
    guestDb.collection("kitchen_projects").doc("proj_guest_1").set(payload)
  );

  await assertSucceeds(
    guestDb
      .collection("kitchen_projects")
      .where("userId", "==", GUEST_UID)
      .get()
  );

  await assertSucceeds(
    guestDb
      .collection("kitchen_projects")
      .doc("proj_guest_1")
      .update({
        name: "مطبخ الضيف المعدل 3D",
        roomWidthCm: 480,
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("Authenticated user: can create, read, query, and update their own kitchen project", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const createdAt = new Date(Date.now() - 5000);
  const payload = validKitchenPayload(ALICE_UID, createdAt);

  await assertSucceeds(
    aliceDb.collection("kitchen_projects").doc("proj_alice_1").set(payload)
  );

  await assertSucceeds(
    aliceDb
      .collection("kitchen_projects")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  // Valid update
  await assertSucceeds(
    aliceDb
      .collection("kitchen_projects")
      .doc("proj_alice_1")
      .update({
        name: "مطبخ الياسمين المعدل",
        totalPriceSar: 31500,
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("Shadow Update & Identity Spoofing: rejects ghost fields and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const createdAt = new Date(Date.now() - 5000);
  const payload = validKitchenPayload(ALICE_UID, createdAt);

  await assertSucceeds(
    aliceDb.collection("kitchen_projects").doc("proj_alice_1").set(payload)
  );

  // Bob cannot read Alice's project
  await assertFails(
    bobDb.collection("kitchen_projects").doc("proj_alice_1").get()
  );

  // Alice cannot inject a ghost field (Shadow Update Test)
  await assertFails(
    aliceDb
      .collection("kitchen_projects")
      .doc("proj_alice_1")
      .update({
        name: "مطبخ مخترق",
        isVerifiedAdmin: true,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Query without userId filter fails
  await assertFails(aliceDb.collection("kitchen_projects").get());
});

test("QuoteRequest: enforces parent KitchenProject ownership and terminal status lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const createdAt = new Date(Date.now() - 5000);

  // Cannot create quote request for non-existent project
  await assertFails(
    aliceDb
      .collection("quote_requests")
      .doc("quote_orphan")
      .set(validQuotePayload(ALICE_UID, "non_existent_proj", createdAt))
  );

  // Create Alice's project first
  await assertSucceeds(
    aliceDb
      .collection("kitchen_projects")
      .doc("proj_alice_1")
      .set(validKitchenPayload(ALICE_UID, createdAt))
  );

  // Bob cannot create a quote referencing Alice's project
  await assertFails(
    bobDb
      .collection("quote_requests")
      .doc("quote_bob_spoof")
      .set(validQuotePayload(BOB_UID, "proj_alice_1", createdAt))
  );

  // Alice can create a quote referencing her own project
  await assertSucceeds(
    aliceDb
      .collection("quote_requests")
      .doc("quote_alice_1")
      .set(validQuotePayload(ALICE_UID, "proj_alice_1", createdAt))
  );

  // Alice can cancel her pending quote
  await assertSucceeds(
    aliceDb
      .collection("quote_requests")
      .doc("quote_alice_1")
      .update({
        status: "cancelled",
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Terminal state lock: once cancelled, cannot update back to pending or edit notes
  await assertFails(
    aliceDb
      .collection("quote_requests")
      .doc("quote_alice_1")
      .update({
        specialNotes: "محاولة تعديل بعد الإلغاء",
        updatedAt: new Date(Date.now() - 500),
      })
  );
});

test("KitchenCatalogUnit: authenticated owner can create, query, update, and isolate catalog items", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const createdAt = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("kitchen_units_catalog")
      .doc("unit_fridge_1")
      .set(validCatalogUnitPayload(ALICE_UID, createdAt))
  );

  // Query aligned with userId filter succeeds
  await assertSucceeds(
    aliceDb
      .collection("kitchen_units_catalog")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  // Query without userId filter fails
  await assertFails(aliceDb.collection("kitchen_units_catalog").get());

  // Cross-user read fails
  await assertFails(
    bobDb.collection("kitchen_units_catalog").doc("unit_fridge_1").get()
  );

  // Valid update succeeds
  await assertSucceeds(
    aliceDb
      .collection("kitchen_units_catalog")
      .doc("unit_fridge_1")
      .update({
        priceSar: 6500,
        materialFinish: "خشب جوز مع ستانلس",
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Shadow update with ghost field fails
  await assertFails(
    aliceDb
      .collection("kitchen_units_catalog")
      .doc("unit_fridge_1")
      .update({
        priceSar: 6500,
        ghostField: "hack",
        updatedAt: new Date(Date.now() - 500),
      })
  );
});
