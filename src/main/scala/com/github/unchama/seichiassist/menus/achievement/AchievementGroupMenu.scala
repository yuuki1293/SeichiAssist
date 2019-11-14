package com.github.unchama.seichiassist.menus.achievement

import cats.data.Kleisli
import cats.effect.IO
import com.github.unchama.itemstackbuilder.{IconItemStackBuilder, SkullItemStackBuilder, SkullOwnerReference}
import com.github.unchama.menuinventory.slot.button.action.LeftClickButtonEffect
import com.github.unchama.menuinventory.slot.button.{Button, RecomputedButton}
import com.github.unchama.menuinventory.{ChestSlotRef, Menu, MenuFrame, MenuSlotLayout}
import com.github.unchama.seichiassist.achievement.SeichiAchievement.{AutoUnlocked, Hidden, ManuallyUnlocked, Normal}
import com.github.unchama.seichiassist.achievement.TitleMapping.TitleCombination
import com.github.unchama.seichiassist.achievement.hierarchy.AchievementGroup
import com.github.unchama.seichiassist.achievement.hierarchy.AchievementGroup._
import com.github.unchama.seichiassist.achievement.{AchievementConditions, SeichiAchievement, TitleMapping}
import com.github.unchama.seichiassist.menus.{ColorScheme, CommonButtons}
import com.github.unchama.seichiassist.{SeichiAssist, SkullOwners}
import com.github.unchama.targetedeffect.player.FocusedSoundEffect
import org.bukkit.ChatColor._
import org.bukkit.entity.Player
import org.bukkit.{Material, Sound}

object AchievementGroupMenu {
  import com.github.unchama.seichiassist.concurrent.PluginExecutionContexts.layoutPreparationContext
  import eu.timepit.refined.auto._

  sealed trait GroupMenuEntry

  case class AchievementEntry(achievement: SeichiAchievement) extends GroupMenuEntry
  object AchievementEntry {
    def within(range: Seq[Int]): List[AchievementEntry] =
      SeichiAchievement.values
        .toList
        .filter(achievement => range.contains(achievement.id))
        .map(AchievementEntry.apply)
  }

  case object Achievement8003UnlockEntry extends GroupMenuEntry

  def sequentialEntriesIn(group: AchievementGroup): List[GroupMenuEntry] =
    group match {
      case BrokenBlockAmount =>
        AchievementEntry.within(3001 to 3009)

      case BrokenBlockRanking =>
        AchievementEntry.within(1001 to 1012)

      case PlayTime =>
        AchievementEntry.within(4001 to 4023) :+
          Achievement8003UnlockEntry

      case TotalLogins =>
        AchievementEntry.within(5001 to 5008)

      case ConsecutiveLogins =>
        AchievementEntry.within(5101 to 5120)

      case Anniversaries =>
        AchievementEntry.within(9001 to 9036)

      case MebiusBreeder =>
        AchievementEntry.within(0 until 0)

      case StarLevel =>
        AchievementEntry.within(0 until 0)

      case OfficialEvent =>
        AchievementEntry.within(7001 to 7027) ++
          AchievementEntry.within(7901 to 7906)

      case VoteCounts =>
        AchievementEntry.within(6001 to 6008)

      case Secrets =>
        AchievementEntry.within(8001 to 8003)
    }

  object Buttons {
    def buttonFor(achievement: SeichiAchievement, hasUnlocked: Boolean): Button = {
      val itemStack = {
        val material = if (hasUnlocked) Material.DIAMOND_BLOCK else Material.BEDROCK
        val title = {
          val displayTitleName =
            TitleMapping.getTitleFor(achievement.id)
              .filter(_ => hasUnlocked)
              .getOrElse("???")

          s"$YELLOW$UNDERLINE${BOLD}No${achievement.id}「$displayTitleName」"
        }

        val lore = {
          val conditionDescriptions =
            achievement match {
              case normal: SeichiAchievement.Normal[_] =>
                List(normal.condition.parameterizedDescription)
              case hidden: SeichiAchievement.Hidden[_] =>
                val description =
                  if (hasUnlocked)
                    hidden.condition.underlying.parameterizedDescription
                  else
                    hidden.condition.maskedDescription
                List(description)
              case SeichiAchievement.GrantedByConsole(_, condition, explanation) =>
                List(condition) ++ explanation.getOrElse(Nil)
            }

          val unlockSchemeDescription =
            achievement match {
              case _: AutoUnlocked =>
                List(s"$RESET$RED※この実績は自動解禁式です。")
              case m: ManuallyUnlocked =>
                m match {
                  case _: Hidden[_] =>
                    List(s"$RESET$RED※この実績は手動解禁式です。")
                  case _ =>
                    if (hasUnlocked)
                      List()
                    else
                      List(s"$RESET$GREEN※クリックで実績に挑戦できます")
                }
              case _ =>
                List(s"$RESET$RED※この実績は配布解禁式です。")
            }

          val hiddenDescription =
            achievement match {
              case _: Hidden[_] => List(s"$RESET${AQUA}こちらは【隠し実績】となります")
              case _ => Nil
            }

          conditionDescriptions.map(s"$RESET$RED" + _) ++
            unlockSchemeDescription ++
            hiddenDescription
        }

        new IconItemStackBuilder(material)
          .title(title)
          .lore(lore)
          .build()
      }

      val clickEffect = {
        import com.github.unchama.targetedeffect.MessageEffects._
        import com.github.unchama.targetedeffect._

        val clickSound = FocusedSoundEffect(Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1f, 1f)

        val effect =
          if (hasUnlocked) {
            def setNickname(player: Player): Unit = {
              val TitleCombination(firstId, secondId, thirdId) =
                TitleMapping.mapping.get(achievement.id) match {
                  case Some(value) => value
                  case None =>
                    player.sendMessage(s"${RED}二つ名の設定に失敗しました。")
                    return
                }

              SeichiAssist
                .playermap(player.getUniqueId)
                .updateNickname(firstId.getOrElse(0), secondId.getOrElse(0), thirdId.getOrElse(0))
              player.sendMessage(s"二つ名「${TitleMapping.getTitleFor(achievement.id).get}」が設定されました。")
            }

            delay(setNickname)
          } else {
            achievement match {
              case _: AutoUnlocked =>
                s"${RED}この実績は自動解禁式です。毎分の処理をお待ちください。".asMessageEffect()
              case achievement: ManuallyUnlocked =>
                achievement match {
                  case achievement: Normal[_] =>
                    Kleisli { player: Player =>
                      for {
                        shouldUnlock <- achievement.condition.shouldUnlock(player)
                        _ <- if (shouldUnlock) IO {
                          SeichiAssist.playermap(player.getUniqueId).TitleFlags.addOne(achievement.id)
                          player.sendMessage(s"実績No${achievement.id}を解除しました！おめでとうございます！")
                        } else {
                          s"${RED}実績No${achievement.id}は条件を満たしていません。".asMessageEffect()(player)
                        }
                      } yield ()
                    }
                  case _ =>
                    s"$RESET$RED※この実績は手動解禁式です。".asMessageEffect()
                }
              case _ =>
                s"$RED※この実績は配布解禁式です。運営チームからの配布タイミングを逃さないようご注意ください。".asMessageEffect()
            }
          }

        sequentialEffect(clickSound, effect)
      }

      Button(itemStack, LeftClickButtonEffect(clickEffect))
    }

    import com.github.unchama.targetedeffect.MessageEffects._
    import com.github.unchama.targetedeffect._

    // 実績8003を解除するためのボタン
    val unlock8003Button: Button = Button(
      new IconItemStackBuilder(Material.EMERALD_BLOCK)
        .title(ColorScheme.navigation("タイムカード、切りましょ？"))
        .lore(s"$RESET$RED※何かが起こります※")
        .build(),
      LeftClickButtonEffect(
        FocusedSoundEffect(Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1f, 1f),
        "お疲れ様でした！今日のお給料の代わりに二つ名をどうぞ！".asMessageEffect(),
        delay { player => SeichiAssist.playermap(player.getUniqueId).TitleFlags.addOne(8003) }
      )
    )

    import com.github.unchama.seichiassist.concurrent.PluginExecutionContexts.layoutPreparationContext

    def computationFor(viewer: Player)(entry: GroupMenuEntry): IO[Button] =
      entry match {
        case AchievementEntry(achievement) => RecomputedButton {
          for {
            hasObtained <- IO { SeichiAssist.playermap(viewer.getUniqueId).TitleFlags.contains(achievement.id) }
            shouldDisplayToUI <- achievement match {
              case hidden: Hidden[_] => hidden.condition.shouldDisplayToUI(viewer)
              case _ => IO.pure(true)
            }
          } yield if (hasObtained || shouldDisplayToUI) buttonFor(achievement, hasObtained) else Button.empty
        }

        case Achievement8003UnlockEntry => RecomputedButton {
          for {
            hasObtained8003 <- IO { SeichiAssist.playermap(viewer.getUniqueId).TitleFlags.contains(8003) }
            shouldDisplayToUI <-
              if (hasObtained8003) IO.pure(false)
              else AchievementConditions.SecretAchievementConditions.unlockConditionFor8003(viewer)
          } yield if (shouldDisplayToUI) unlock8003Button else Button.empty
        }
      }
  }

  def apply(group: AchievementGroup, pageNumber: Int = 1): Menu = {
    val entriesToDisplay = {
      val displayPerPage = 3 * 9
      val displayFromIndex = displayPerPage * (pageNumber - 1)
      val displayUptoIndex = displayFromIndex + displayPerPage

      sequentialEntriesIn(group).slice(displayFromIndex, displayUptoIndex)
    }

    val groupAchievementsCount = group.achievements.size
    val maxPageNumber = Math.ceil(groupAchievementsCount / 27.0).toInt

    if (entriesToDisplay.isEmpty) {
      if (groupAchievementsCount == 0) {
        AchievementCategoryMenu(group.parent)
      } else {
        apply(group, maxPageNumber)
      }
    } else {
      new Menu {
        import com.github.unchama.menuinventory.InventoryRowSize._

        override val frame: MenuFrame = MenuFrame(4.rows, s"$YELLOW$UNDERLINE${BOLD}実績「${group.name}」")

        override def computeMenuLayout(player: Player): IO[MenuSlotLayout] = {
          val toCategoryMenuButtonSection = Map(
            9 * 3 -> CommonButtons.transferButton(
              new SkullItemStackBuilder(SkullOwners.MHF_ArrowLeft),
              s"「${group.parent.name}」カテゴリメニューへ",
              AchievementCategoryMenu(group.parent)
            )
          )

          def buttonToTransferTo(newPageNumber: Int, skullOwnerReference: SkullOwnerReference): Button =
            CommonButtons.transferButton(
              new SkullItemStackBuilder(skullOwnerReference),
              s"${newPageNumber}ページ目へ",
              AchievementGroupMenu(group, newPageNumber)
            )

          val previousPageButtonSection =
            if (pageNumber > 1) {
              Map(ChestSlotRef(3, 7) -> buttonToTransferTo(pageNumber - 1, SkullOwners.MHF_ArrowLeft))
            } else {
              Map()
            }

          val nextPageButtonSection =
            if (pageNumber < maxPageNumber) {
              Map(ChestSlotRef(3, 8) -> buttonToTransferTo(pageNumber + 1, SkullOwners.MHF_ArrowRight))
            } else {
              Map()
            }

          import cats.implicits._

          val dynamicPartComputation =
            entriesToDisplay
              .traverse(Buttons.computationFor(player))
              .map(_.zipWithIndex.map(_.swap))

          for {
            dynamicPart <- dynamicPartComputation
            combinedLayout =
              toCategoryMenuButtonSection ++
                previousPageButtonSection ++
                nextPageButtonSection ++
                dynamicPart
          } yield MenuSlotLayout(combinedLayout)
        }
      }
    }
  }
}