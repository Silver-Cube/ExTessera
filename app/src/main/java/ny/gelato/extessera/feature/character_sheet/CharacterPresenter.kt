package ny.gelato.extessera.feature.character_sheet

import android.view.MenuItem
import android.view.View
import ny.gelato.extessera.R
import ny.gelato.extessera.base.BasePresenter
import ny.gelato.extessera.base.BaseViewModel
import ny.gelato.extessera.data.model.character.Character
import ny.gelato.extessera.data.model.character.Preferences
import ny.gelato.extessera.data.source.CharacterManager
import ny.gelato.extessera.feature.character_sheet.view_model.*
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import javax.inject.Inject


/**
 * Created by jord.goldberg on 4/30/17.
 */

class CharacterPresenter @Inject constructor(val characterManager: CharacterManager) : BasePresenter<CharacterView>() {

    val subscriptions = CompositeSubscription()

    val feed = Observable.Transformer<Character, MutableList<BaseViewModel>> {
        it.filter { it.isValid }.map { character ->
            mutableListOf<BaseViewModel>().apply {
                add(AvatarModel(character))
                if (character.preferences.showNotes || character.hasToLevelUp())
                    addAll(character.noteModels())
                if (character.hp == 0)
                    add(DeathSaveModel(character))
                add(StatusModel(character))
                add(AbilitiesModel(character))
                add(SavingThrowsModel(character))
                addAll(character.skillModels())
                addAll(character.equipmentModels())
                addAll(character.weaponModels())
                if (character.preferences.showSpells)
                    addAll(character.spellModels())
            }
        }.observeOn(AndroidSchedulers.mainThread())
    }

    lateinit var character: Character

    fun start() {
        val subscription = characterManager
                .getCharacter()
                .doOnNext { character = it }
                .compose(feed)
                .subscribe(
                        { feed -> view.showCharacter(feed) },
                        { t -> t.printStackTrace() })

        subscriptions.add(subscription)
    }

    fun stop() = subscriptions.clear()

    fun save(model: BaseViewModel) {
        when (model) {
            is AvatarModel -> characterManager.updateAvatar(model)
            is ExpModel -> characterManager.updateExp(model)
            is LevelUpModel -> characterManager.updateLevel(model)
            is NoteModel -> characterManager.createNote(model)
            is StatusModel -> characterManager.updateStatus(model)
            is HpModel -> characterManager.updateHp(model)
            is MaxHpModel -> characterManager.updateMaxHp(model)
            is AbilitiesModel -> characterManager.updateAbilities(model)
            is SavingThrowsModel -> characterManager.updateSaves(model)
            is SkillModel -> characterManager.updateSkill(model)
            is WeaponModel -> characterManager.addWeapon(model.name)
            is SpellModel -> characterManager.updateSpell(model)
            is CoinModel -> characterManager.updateCoin(model)
            is EquipmentModel -> characterManager.createEquipment(model)
        }
    }

    fun delete(model: BaseViewModel) {
        when (model) {
            is NoteModel -> characterManager.deleteNote(model)
            is WeaponModel -> characterManager.removeWeapon(model.name)
            is SpellModel -> characterManager.forgetSpell(model.name)
            is EquipmentModel -> characterManager.deleteEquipment(model)
        }
    }

    fun click(v: View, model: BaseViewModel) {
        when (model) {
            is AvatarModel ->
                if (model.editable) view.showPopupMenu(v, R.menu.menu_character_avatar)
                else if (view.isAtScrollTop()) view.showImageSelect(model)
                else view.showScrollToTop()
            is AboutModel -> view.showAbout(AboutModel(character))
            is ExpModel -> view.showAddExp(ExpModel(character.exp, character.expToNextLevel()))
            is GoToModel -> when (model.destination) {
                GoToModel.Destination.NONE -> view.showGoTo(model)
                GoToModel.Destination.NOTES -> {
                    if (!character.preferences.showNotes)
                        characterManager.updatePreference(Preferences.Toggle.SHOW_NOTES)
                }
                GoToModel.Destination.ABILITIES -> view.showScrollToDestination(AbilitiesModel())
                GoToModel.Destination.SAVES -> view.showScrollToDestination(SavingThrowsModel())
                GoToModel.Destination.SKILLS -> view.showScrollToDestination(SkillModel())
                GoToModel.Destination.WEAPONS -> view.showScrollToDestination(WeaponModel())
                GoToModel.Destination.SPELLS -> {
                    if (!character.preferences.showSpells)
                        characterManager.updatePreference(Preferences.Toggle.SHOW_SPELLS)
                    view.showScrollToDestination(SpellModel())
                }
                GoToModel.Destination.EQUIPMENT -> view.showScrollToDestination(CoinModel())
            }
            is NoteModel ->
                if (model.isEmpty()) view.showCreateNote()
                else characterManager.updateNote(model)
            is DeathSaveModel ->
                if (model.editable) view.showPopupMenu(v, R.menu.menu_character_death_saves)
                else {
                    characterManager.updateDeathSaves(model)
                    if (model.successes == 3) view.showIsStabilized()
                }
            is StatusModel ->
                if (model.isDcEditable) view.showSelectDcAbility(model)
                else view.showPopupMenu(v, R.menu.menu_character_status)
            is HpModel -> view.showEditHp(HpModel(character))
            is SkillModel -> view.showSelectSkillProficiency(model)
            is WeaponModel -> view.showWeaponDetail(model)
            is SpellModel ->
                if (model.isEmpty()) view.showSpellsFor(character)
                else view.showSpellDetail(model)
            is CoinModel -> view.showCoin(model)
            is EquipmentModel ->
                if (model.editable) characterManager.updateEquipment(model)
                else if (model.isEmpty()) view.showCreateEquipment()
                else view.showEquipmentItem(model)
            is EquipmentFooterModel ->
                if (model.equipmentSize > CoinModel.Type.values().size) null
                else view.showCreateEquipment()
            is HeaderModel -> view.showPopupMenu(v, model.menuRes)
        }
    }

    fun longClick(model: BaseViewModel): Boolean = when (model) {
        is AvatarModel -> {
            characterManager.updateAvatar(model)
            if (model.isInspired) view.showHasInspiration(model)
            true
        }
        is StatusModel -> {
            characterManager.updateStatus(model)
            true
        }
        is MaxHpModel -> {
            view.showEditMaxHp(MaxHpModel(character))
            true
        }
        is AbilitiesModel -> {
            characterManager.updateAbilities(model)
            true
        }
        is SavingThrowsModel -> {
            characterManager.updateSaves(model)
            true
        }
        is SpellModel -> {
            characterManager.updateSpell(model)
            true
        }
        else -> false
    }

    fun menuClick(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.action_avatar_edit_character -> view.showEditCharacter(character)
            R.id.action_avatar_select_picture -> view.showImageSelect(AvatarModel(character))
            R.id.action_avatar_toggle_inspiration -> {
                val avatar = AvatarModel(character).apply { isInspired = !isInspired }
                characterManager.updateAvatar(avatar)
                if (avatar.isInspired) view.showHasInspiration(avatar)
            }
            R.id.action_notes_create -> view.showCreateNote()
            R.id.action_notes_clear_checked -> characterManager.deleteCheckedNotes()
            R.id.action_notes_hide -> characterManager.updatePreference(Preferences.Toggle.SHOW_NOTES)
            R.id.action_death_save_reset -> characterManager.updateDeathSaves(DeathSaveModel())
            R.id.action_death_save_stabilize -> {
                characterManager.updateDeathSaves(DeathSaveModel(successes = 3))
                view.showIsStabilized()
            }
            R.id.action_status_short_rest -> null
            R.id.action_status_long_rest -> null
            R.id.action_status_edit_max_hp -> view.showEditMaxHp(MaxHpModel(character))
            R.id.action_skills_toggle_sort -> characterManager.updatePreference(Preferences.Toggle.SORT_SKILLS)
            R.id.action_weapons_add -> view.showWeaponsFor(character)
            R.id.action_spells_add -> view.showSpellsFor(character)
            R.id.action_spells_hide -> characterManager.updatePreference(Preferences.Toggle.SHOW_SPELLS)
            R.id.action_equipment_add -> view.showCreateEquipment()
        }
    }
}
