"""All in-game text (CLEO FXT). Keys are at most 7 characters.

Colour codes: ~r~ red, ~g~ green, ~y~ yellow, ~b~ blue, ~w~ white, ~n~ new line, ~1~ number.
"""

TEXT = {
    # ---------------------------------------------------------------- zones (info.zon text labels)
    "AFZ_QN": "Qala-e-Naw",
    "AFZ_CS": "Camp Sangar",
    "AFZ_DB": "Deh-e-Bagh",
    "AFZ_DS": "Dara-e-Sabz",
    "AFZ_SK": "Sar-e-Kotal",
    "AFZ_SH": "Shahrak",
    "AFZ_SS": "Sang-e-Siah",
    "AFZ_QK": "Qala-e-Kohna",
    "AFZ_DC": "Darwaza Checkpoint",
    "AFZ_BK": "Burj-e-Khak",
    "AFZ_PL": "Shomali Plain",
    "AFZ_GV": "Ghorband Valley",
    "AFZ_NM": "Koh-e-Shamal",
    "AFZ_SM": "Paghman Range",

    # ---------------------------------------------------------------- vehicle names (vehicles.ide GXT keys)
    "RANGER": "Police Pickup",
    "PATRIOT": "Army Jeep",
    "BARRCKS": "Army Truck",
    "SANDKIN": "Desert Pickup",

    # ---------------------------------------------------------------- general
    "AFG_TTL": "THE VALLEY",
    "AFG_INT": "Shomali plain, north of Kabul. ~y~Farid Noori~w~ is home after eight years away.",
    "AFG_I2": "His uncle ~y~Kaka Rahim~w~ is waiting at the ~y~bazaar~w~.",
    "AFG_SAV": "Stand in the marker in your courtyard to ~g~save~w~ the game.",
    "AFG_WRD": "Wardrobe: step into the marker to change clothes. Some outfits unlock as you help the valley.",
    "AFG_WR1": "Outfit: ~y~villager~w~",
    "AFG_WR2": "Outfit: ~y~farmer~w~",
    "AFG_WR3": "Outfit: ~y~shepherd~w~",
    "AFG_WR4": "Outfit: ~y~city clothes~w~",
    "AFG_WR5": "Outfit: ~y~police volunteer~w~",
    "AFG_WR6": "Outfit: ~y~army fatigues~w~",
    "AFG_WRL": "~r~Locked~w~ - finish more missions first.",
    "AFG_RAD": "Afghan radio: ~y~N~w~ next track, ~y~M~w~ radio on/off.",
    "AFG_END": "The valley is quiet again. ~y~Free roam~w~: the Hunter at Camp Sangar is yours.",
    "AFG_CON": "Missions: walk into the ~y~marker~w~ of the contact shown on the radar.",

    # ---------------------------------------------------------------- mission titles
    "AF1_T": "Homecoming",
    "AF2_T": "Market Day",
    "AF3_T": "Medicine for Shahrak",
    "AF4_T": "Darwaza",
    "AF5_T": "Aid Convoy",
    "AF6_T": "Black Stone",
    "AF7_T": "Eagle's Nest",
    "AF8_T": "Qala-e-Kohna",

    # ---------------------------------------------------------------- 1 Homecoming
    "AF1_01": "~y~Rahim:~w~ Farid! Eight years! Your mother would weep to see you so thin.",
    "AF1_02": "~y~Rahim:~w~ No time for tea. The baker in ~y~Deh-e-Bagh~w~ needs this flour before evening.",
    "AF1_03": "Get in the ~b~pickup~w~.",
    "AF1_04": "Drive the flour to ~y~Deh-e-Bagh~w~.",
    "AF1_05": "~y~Rahim:~w~ Careful with my truck. It is older than you.",
    "AF1_06": "~r~The pickup is wrecked!",
    "AF1_07": "~y~Baker:~w~ Rahim's nephew? Tell him the Black Stone men took my last delivery at gunpoint.",

    # ---------------------------------------------------------------- 2 Market Day
    "AF2_01": "~y~Rahim:~w~ Those are ~r~Black Stone~w~ men. They 'tax' every stall in the bazaar.",
    "AF2_02": "~y~Rahim:~w~ Take your father's pistol. Chase them out!",
    "AF2_03": "Deal with the ~r~extortionists~w~.",
    "AF2_04": "~y~Rahim:~w~ They will be back with friends. Their camp is up the ~y~Ghorband valley~w~.",

    # ---------------------------------------------------------------- 3 Medicine
    "AF3_01": "~y~Rahim:~w~ The clinic in ~y~Shahrak~w~ has a sick child and no medicine.",
    "AF3_02": "~y~Rahim:~w~ Take the pickup. The doctor needs it ~r~before the time runs out~w~.",
    "AF3_03": "Deliver the medicine to ~y~Shahrak~w~.",
    "AF3_04": "~r~Too late!",
    "AF3_05": "~y~Doctor:~w~ Allah bless you. Captain ~y~Nasir~w~ at the police post wants to meet you.",

    # ---------------------------------------------------------------- 4 Darwaza
    "AF4_01": "~y~Nasir:~w~ You are the one who chased the Black Stone out of the bazaar? Good.",
    "AF4_02": "~y~Nasir:~w~ They plan to hit ~y~Darwaza Checkpoint~w~ tonight. I have three men there.",
    "AF4_03": "Go to ~y~Darwaza Checkpoint~w~.",
    "AF4_04": "Hold the checkpoint! Wave ~1~ of 3.",
    "AF4_05": "~y~Nasir:~w~ The checkpoint held. Take the rifle, you earned it.",
    "AF4_06": "~r~You left the checkpoint!",

    # ---------------------------------------------------------------- 5 Aid Convoy
    "AF5_01": "~y~Major Hamid:~w~ Nasir speaks well of you. I need a driver who knows the valley.",
    "AF5_02": "~y~Hamid:~w~ Take the aid truck to ~y~Dara-e-Sabz~w~. The road is not safe.",
    "AF5_03": "Get in the ~b~aid truck~w~.",
    "AF5_04": "Drive the aid to ~y~Dara-e-Sabz~w~.",
    "AF5_05": "~r~Ambush!~w~ Protect the truck.",
    "AF5_06": "~r~The aid truck was destroyed!",
    "AF5_07": "~y~Elder:~w~ Flour, blankets and medicine. The village will remember this.",

    # ---------------------------------------------------------------- 6 Black Stone
    "AF6_01": "~y~Nasir:~w~ We found their camp at ~y~Sang-e-Siah~w~, at the head of the valley.",
    "AF6_02": "~y~Nasir:~w~ My men cannot go that far. Clear the camp and bring back what they stole.",
    "AF6_03": "Clear the bandit camp at ~y~Sang-e-Siah~w~.",
    "AF6_04": "Bandits left: ~1~",
    "AF6_05": "~y~Nasir (radio):~w~ The camp is ours. Their leader ~r~Siahdast~w~ escaped to the old fortress.",

    # ---------------------------------------------------------------- 7 Eagle's Nest
    "AF7_01": "~y~Hamid:~w~ Siahdast is hiding in ~y~Qala-e-Kohna~w~, high on the ridge.",
    "AF7_02": "~y~Hamid:~w~ Take the ~b~helicopter~w~ and scout the fortress. Do not land there!",
    "AF7_03": "Fly over ~y~Qala-e-Kohna~w~.",
    "AF7_04": "~r~RPG!~w~ Get out of there and fly back to ~y~Camp Sangar~w~.",
    "AF7_05": "Land on the ~y~helipad~w~ at Camp Sangar.",
    "AF7_06": "~r~The helicopter is lost!",
    "AF7_07": "~y~Hamid:~w~ Good pictures. Now we know where his guards stand.",

    # ---------------------------------------------------------------- 8 Qala-e-Kohna
    "AF8_01": "~y~Hamid:~w~ We go tonight. My soldiers will follow you up the mountain.",
    "AF8_02": "Take ~y~Qala-e-Kohna~w~. Kill ~r~Siahdast~w~.",
    "AF8_03": "~y~Siahdast:~w~ This valley was mine before you were born, boy!",
    "AF8_04": "Enemies left: ~1~",
    "AF8_05": "~y~Hamid:~w~ It is over. Go home, Farid. Your uncle has tea waiting.",
}
