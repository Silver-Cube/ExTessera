package ny.gelato.extessera.data.model.character

import io.realm.RealmList
import io.realm.RealmObject

/**
 * Created by jord.goldberg on 5/8/17.
 */

open class Job(
        var job: String = Job.Type.FIGHTER.name,
        var level: Int = 1,
        var dice: Int = 1,
        var features: RealmList<Feature> = RealmList()

) : RealmObject() {

    enum class Type(val formatted: String) {
        BARBARIAN("Barbarian"),
        BARD("Bard"),
        CLERIC("Cleric"),
        DRUID("Druid"),
        FIGHTER("Fighter"),
        MONK("Monk"),
        PALADIN("Paladin"),
        RANGER("Ranger"),
        ROGUE("Rogue"),
        SORCERER("Sorcerer"),
        WARLOCK("Warlock"),
        WIZARD("Wizard")
    }

    enum class Archetype(val formatted: String) {

        BERSERKER("Berserker") {
            override fun type(): Type = Type.BARBARIAN
        },
        TOTEM_WARRIOR("Totem Warrior"){
            override fun type(): Type = Type.BARBARIAN
        },
        COLLEGE_OF_LORE("College of Lore"){
            override fun type(): Type = Type.BARD
        },
        COLLEGE_OF_VALOR("College of Valor"){
            override fun type(): Type = Type.BARD
        },

        ;

        abstract fun type(): Type
    }

    fun playersHandbookPage(): Int = when (Type.valueOf(job)) {
        Job.Type.BARBARIAN -> 46
        Job.Type.BARD -> 51
        Job.Type.CLERIC -> 56
        Job.Type.DRUID -> 64
        Job.Type.FIGHTER -> 70
        Job.Type.MONK -> 76
        Job.Type.PALADIN -> 82
        Job.Type.RANGER -> 89
        Job.Type.ROGUE -> 94
        Job.Type.SORCERER -> 99
        Job.Type.WARLOCK -> 105
        Job.Type.WIZARD -> 112
    }

    fun hitDieMax(): Int = when (Type.valueOf(job)) {
        Type.BARBARIAN -> 12
        Type.FIGHTER, Type.PALADIN, Type.RANGER -> 10
        Type.BARD, Type.CLERIC, Type.DRUID, Type.MONK, Type.ROGUE, Type.WARLOCK -> 8
        Type.SORCERER, Type.WIZARD -> 6
    }

    fun hitDieMaxFormatted(): String = "d${hitDieMax()}"

    fun hitDiceFormatted(): String = "$dice${hitDieMaxFormatted()}"

    fun castingAbility(): Ability.Type = when (Type.valueOf(job)) {
        Type.BARD, Type.PALADIN, Type.SORCERER, Type.WARLOCK -> Ability.Type.CHA
        Type.CLERIC, Type.DRUID, Type.MONK, Type.RANGER -> Ability.Type.WIS
        Type.FIGHTER, Type.ROGUE, Type.WIZARD -> Ability.Type.INT
        else -> Ability.Type.STR
    }

    fun casterLevel(): Int = when (Type.valueOf(job)) {
        Type.BARD, Type.CLERIC, Type.DRUID, Type.SORCERER, Type.WIZARD -> level
        Type.PALADIN, Type.RANGER -> level / 2
        Type.ROGUE, Type.FIGHTER -> level / 3
        Type.WARLOCK -> level
        else -> 0
    }

    fun spellLevel(): Int = when (casterLevel()) {
        1, 2 -> 1
        3, 4 -> 2
        5, 6 -> 3
        7, 8 -> 4
        9, 10 -> 5
        11, 12 -> 6
        13, 14 -> 7
        15, 16 -> 8
        in 17..20 -> 9
        else -> 0
    }

    fun resetLevelTo(level: Int) {
        features.deleteAllFromRealm()
        for (i in 1..level) {
            this.level = i
            addOrUpdateFeatures(featuresForLevel())
        }
    }

    fun levelNotes(): List<Note> = ArrayList<Note>().apply {
        val features = featuresForLevel()

        if (features.isNotEmpty()) {
            addOrUpdateFeatures(features)
            add(Note(text = "${Type.valueOf(job).formatted} $level " +
                    "${if (features.size > 1) "features" else "feature"} " +
                    "(PHB ${playersHandbookPage()}):\n" +
                    features.map { it.name }.toString().substring(1).dropLast(1)))
        }

        when (level) {
            4, 8, 12, 16, 19 -> add(Note(text = "Ability Score Improvement"))
        }

        when (Type.valueOf(job)) {
            Type.BARBARIAN -> when (level) {
                6, 10, 14 -> add(Note(text = "Choose Path feature"))
            }
            Type.BARD -> when (level) {
                3 -> add(Note(text = "Expertise: choose 2 skill proficiencies"))
                6 -> add(Note(text = "Choose Bard College feature"))
                10 -> {
                    add(Note(text = "Expertise: choose 2 skill proficiencies"))
                    add(Note(text = "Magical Secrets: learn 2 spells"))
                }
                14 -> {
                    add(Note(text = "Magical Secrets: learn 2 spells"))
                    add(Note(text = "Choose Bard College feature"))
                }
                18 -> add(Note(text = "Magical Secrets: learn 2 spells"))
            }
            Type.CLERIC -> when (level) {
                2, 6, 8, 17 -> add(Note(text = "Choose Divine Domain feature"))
            }
            Type.DRUID -> when (level) {
                4, 8 -> add(Note(text = "Wild Shape improvement"))
                6, 10, 14 -> add (Note(text = "Choose Druid Circle feature"))

                18 -> add(Note(text = "Magical Secrets: learn 2 spells"))
            }
            Type.FIGHTER -> when (level) {
                6 -> add(Note(text = "Ability Score Improvement"))
                7, 10, 15, 18 -> add(Note(text = "Choose Martial Archetype feature"))
            }
            Type.MONK -> when (level) {
                6, 11, 17 -> add(Note(text = "Choose Monastic Tradition feature"))
            }
            Type.PALADIN -> when (level) {
                7, 15, 20 -> add(Note(text = "Choose Sacred Oath feature"))
            }
            Type.RANGER -> when (level) {
                6 -> {
                    add(Note(text = "Favored Enemy improvement"))
                    add(Note(text = "Natural Explorer improvement"))
                }
                7, 11, 15 -> add(Note(text = "Choose Ranger Archetype feature"))
                10 -> add(Note(text = "Natural Explorer improvement"))
                14 -> add(Note(text = "Favored Enemy improvement"))
            }
            Type.ROGUE -> when (level) {
                1, 6 -> add(Note(text = "Expertise: choose 2 skill proficiencies"))
                9, 13, 17 -> add(Note(text = "Choose Roguish Archetype feature"))
                10 -> add(Note(text = "Ability Score Improvement"))
            }
            Job.Type.SORCERER -> when (level) {
                10, 17 -> add(Note(text = "Choose Metamagic option"))
                6, 14, 18 -> add(Note(text = "Choose Sorcerous Origin feature"))
            }
            Job.Type.WARLOCK -> when (level) {
                5, 7, 9, 12, 18 -> add(Note(text = "Choose additional Eldtritch Invocation"))
                6, 10, 14 -> add(Note(text = "Choose Otherworldly Patron feature"))
                11, 13, 17 -> add(Note(text = "Choose ${spellLevel()}th Mystic Arcanum"))
                15 -> {
                    add(Note(text = "Choose additional Eldtritch Invocation"))
                    add(Note(text = "Choose ${spellLevel()}th Mystic Arcanum"))
                }
            }
            Job.Type.WIZARD -> when (level) {
                6, 10, 14 -> add(Note(text = "Choose Arcane Tradition feature"))
            }
        }
    }

    private fun featuresForLevel(): List<Feature> = ArrayList<Feature>().apply {
        when (Type.valueOf(job)) {
            Type.BARBARIAN -> when (level) {
                1 -> {
                    add(Feature("Rage"))
                    add(Feature("Unarmored Defense"))
                }
                2 -> {
                    add(Feature("Reckless Attack"))
                    add(Feature("Danger Sense"))
                }
                3 -> add(Feature("Primal Path"))
                5 -> {
                    add(Feature("Extra Attack"))
                    add(Feature("Fast Movement"))
                }
                7 -> add(Feature("Feral Instinct"))
                9 -> add(Feature("Brutal Critical (+1d)"))
                11 -> add(Feature("Relentless Rage"))
                13 -> add(Feature("Brutal Critical (+2d)"))
                15 -> add(Feature("Persistent Rage"))
                17 -> add(Feature("Brutal Critical (+3d)"))
                18 -> add(Feature("Indomitable Might"))
                20 -> add(Feature("Primal Champion"))
            }
            Type.BARD -> when (level) {
                1 -> {
                    add(Feature("Spellcasting"))
                    add(Feature("Bardic Inspiration (d6)"))
                }
                2 -> {
                    add(Feature("Jack of All Trades"))
                    add(Feature("Song of Rest (d6)"))
                }
                3 -> add(Feature("Bard College"))
                5 -> {
                    add(Feature("Bardic Inspiration (d8)"))
                    add(Feature("Font of Inspiration"))
                }
                6 -> add(Feature("Countercharm"))
                9 -> add(Feature("Song of Rest d8"))
                10 -> add(Feature("Bardic Inspiration (d10)"))
                13 -> add(Feature("Song of Rest (d10)"))
                15 -> add(Feature("Bardic Inspiration (d12)"))
                17 -> add(Feature("Song of Rest (d12)"))
                20 -> add(Feature("Superior Inspiration"))
            }
            Type.CLERIC -> when (level) {
                1 -> {
                    add(Feature("Spellcasting"))
                    add(Feature("Divine Domain"))
                }
                2 -> add(Feature("Channel Divinity (1/rest)"))
                5 -> add(Feature("Destroy Undead (CR 1/2)"))
                6 -> add(Feature("Channel Divinity (2/rest)"))
                8 -> add(Feature("Destroy Undead (CR 1)"))
                10 -> add(Feature("Divine Intervention"))
                11 -> add(Feature("Destroy Undead (CR 2)"))
                14 -> add(Feature("Destroy Undead (CR 3)"))
                17 -> add(Feature("Destroy Undead (CR 4)"))
                18 -> add(Feature("Channel Divinity (3/rest)"))
                20 -> add(Feature("Divine Intervention improvement"))
            }
            Type.DRUID -> when (level) {
                1 -> {
                    add(Feature("Druidic"))
                    add(Feature("Spellcasting"))
                }
                2 -> {
                    add(Feature("Wild Shape"))
                    add(Feature("Druid Circle"))
                }
                18 -> {
                    add(Feature("Timeless Body"))
                    add(Feature("Beast Spells"))
                }
                20 -> add(Feature("Archdruid"))
            }
            Type.FIGHTER -> when (level) {
                1 -> {
                    add(Feature("Fighting Style"))
                    add(Feature("Second Wind"))
                }
                2 -> add(Feature("Action Surge (one use)"))
                3 -> add(Feature("Martial Archetype"))
                5 -> add(Feature("Extra Attack (1)"))
                9 -> add(Feature("Indomitable (one use)"))
                11 -> add(Feature("Extra Attack (2)"))
                13 -> add(Feature("Indomitable (two uses)"))
                15 -> add(Feature("Persistent Rage"))
                17 -> {
                    add(Feature("Action Surge (two uses)"))
                    add(Feature("Indomitable (three uses)"))
                }
                20 -> add(Feature("Extra Attack (3)"))
            }
            Type.MONK -> when (level) {
                1 -> {
                    add(Feature("Unarmored Defense"))
                    add(Feature("Martial Arts"))
                }
                2 -> {
                    add(Feature("Ki"))
                    add(Feature("Unarmored Movement"))
                }
                3 -> {
                    add(Feature("Monastic Tradition"))
                    add(Feature("Deflect Missiles"))
                }
                4 -> add(Feature("Slow Fall"))
                5 -> {
                    add(Feature("Extra Attack"))
                    add(Feature("Stunning Strike"))
                }
                6 -> add(Feature("Ki Empowered Strikes"))
                7 -> {
                    add(Feature("Evasion"))
                    add(Feature("Stillness of Mind"))
                }
                9 -> add(Feature("Unarmored Movement improvement"))
                10 -> add(Feature("Purity of Body"))
                13 -> add(Feature("Tongue of the Sun and Moon"))
                14 -> add(Feature("Diamond Soul"))
                15 -> add(Feature("Timeless Body"))
                18 -> add(Feature("Empty Body"))
                20 -> add(Feature("Perfect Self"))
            }
            Type.PALADIN -> when (level) {
                1 -> {
                    add(Feature("Divine Sense"))
                    add(Feature("Lay on Hands"))
                }
                2 -> {
                    add(Feature("Fighting Style"))
                    add(Feature("Spellcasting"))
                    add(Feature("Divine Smite"))
                }
                3 -> {
                    add(Feature("Divine Health"))
                    add(Feature("Sacred Oath"))
                }
                5 -> add(Feature("Extra Attack"))
                6 -> add(Feature("Aura of Protection"))
                10 -> add(Feature("Aura of Courage"))
                11 -> add(Feature("Improved Divine Smite"))
                14 -> add(Feature("Cleansing Touch"))
                18 -> add(Feature("Aura improvements"))
            }

            Job.Type.RANGER -> when (level) {
                1 -> {
                    add(Feature("Favored Enemy"))
                    add(Feature("Natural Explorer"))
                }
                2 -> {
                    add(Feature("Fighting Style"))
                    add(Feature("Spellcasting"))
                }
                3 -> {
                    add(Feature("Ranger Archetype"))
                    add(Feature("Primeval Awareness"))
                }
                5 -> add(Feature("Extra Attack"))
                8 -> add(Feature("Land's Stride"))
                10 -> add(Feature("Hide in Plain Sight"))
                14 -> add(Feature("Vanish"))
                18 -> add(Feature("Feral Senses"))
                20 -> add(Feature("Foe Slayer"))
            }
            Job.Type.ROGUE -> when (level) {
                1 -> {
                    add(Feature("Sneak Attack"))
                    add(Feature("Thieves' Cant"))
                }
                2 -> add(Feature("Cunning Action"))
                3 -> add(Feature("Roguish Archetype"))
                5 -> add(Feature("Uncanny Dodge"))
                7 -> add(Feature("Evasion"))
                11 -> add(Feature("Reliable Talent"))
                14 -> add(Feature("Blind Sense"))
                15 -> add(Feature("Slippery Mind"))
                18 -> add(Feature("Elusive"))
                20 -> add(Feature("Stroke of Luck"))
            }
            Job.Type.SORCERER -> when (level) {
                1 -> {
                    add(Feature("Spellcasting"))
                    add(Feature("Sorcerous Origin"))
                }
                2 -> add(Feature("Font of Magic"))
                3 -> add(Feature("Metamagic"))
                20 -> add(Feature("Sorcerous Restoration"))
            }
            Job.Type.WARLOCK -> when (level) {
                1 -> {
                    add(Feature("Otherworldly Patron"))
                    add(Feature("Pact Magic"))
                }
                2 -> add(Feature("Eldritch Invocations"))
                3 -> add(Feature("Pact Boon"))
                11 -> add(Feature("Mystic Arcanum"))
                20 -> add(Feature("Eldritch Master"))
            }
            Job.Type.WIZARD -> when (level) {
                1 -> {
                    add(Feature("Spellcasting"))
                    add(Feature("Arcane Recovery"))
                }
                2 -> add(Feature("Arcane Tradition"))
                18 -> add(Feature("Spell Mastery"))
                20 -> add(Feature("Signature Spell"))
            }
        }
    }

    private fun addOrUpdateFeatures(new: List<Feature>) {
        for (feature in new) {
            if (feature.name.contains(" (")) {
                val old = features.where().contains("name", feature.name.substringBefore(" (")).findFirst()
                old?.let { features.remove(it); it.deleteFromRealm() }
            }
            features.add(feature)
        }
    }
}