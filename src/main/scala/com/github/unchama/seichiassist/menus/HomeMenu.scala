package com.github.unchama.seichiassist.menus

import cats.effect.{ConcurrentEffect, IO}
import com.github.unchama.itemstackbuilder.IconItemStackBuilder
import com.github.unchama.menuinventory.router.CanOpen
import com.github.unchama.menuinventory.slot.button.Button
import com.github.unchama.menuinventory.slot.button.action.LeftClickButtonEffect
import com.github.unchama.menuinventory.{ChestSlotRef, Menu, MenuFrame, MenuSlotLayout}
import com.github.unchama.seichiassist.ManagedWorld
import com.github.unchama.seichiassist.subsystems.home.HomeReadAPI
import com.github.unchama.seichiassist.subsystems.home.domain.{Home, HomeId}
import com.github.unchama.targetedeffect._
import com.github.unchama.targetedeffect.player.PlayerEffects._
import com.github.unchama.targetedeffect.player.{CommandEffect, FocusedSoundEffect}
import org.bukkit.ChatColor._
import org.bukkit.entity.Player
import org.bukkit.{Material, Sound}

/**
 * ホームメニュー
 *
 * Created by karayuu on 2019/12/14
 */
object HomeMenu extends Menu {

  import com.github.unchama.menuinventory.syntax._
  import com.github.unchama.seichiassist.concurrent.PluginExecutionContexts.onMainThread
  import eu.timepit.refined.auto._

  class Environment(
    implicit val ioCanOpenConfirmationMenu: IO CanOpen HomeChangeConfirmationMenu,
    val ioCanOpenHomeRemoveConfirmationMenu: IO CanOpen HomeRemoveConfirmationMenu,
    val ioCanReadHome: HomeReadAPI[IO]
  )

  /**
   * メニューのサイズとタイトルに関する情報
   */
  override val frame: MenuFrame = MenuFrame(4.chestRows, s"$DARK_PURPLE${BOLD}ホームメニュー")

  /**
   * @return
   *   `player`からメニューの[[MenuSlotLayout]]を計算する[[IO]]
   */
  override def computeMenuLayout(
    player: Player
  )(implicit environment: Environment): IO[MenuSlotLayout] = {
    import eu.timepit.refined._
    import eu.timepit.refined.auto._
    import eu.timepit.refined.numeric._

    val buttonComputations = ButtonComputations(player)
    import buttonComputations._

    val homePointPart = for {
      homeNumber <- 1 to Home.maxHomePerPlayer
    } yield {
      val column = refineV[Interval.ClosedOpen[0, 9]](homeNumber - 1)
      column match {
        case Right(value) =>
          Map(
            ChestSlotRef(0, value) -> ConstantButtons.warpToHomePointButton(homeNumber),
            ChestSlotRef(2, value) -> ConstantButtons.setHomeButton(homeNumber),
            ChestSlotRef(3, value) -> ConstantButtons.removeHomeButton(homeNumber)
          )
        case Left(_) => throw new RuntimeException("This branch should not be reached.")
      }
    }

    import cats.implicits._
    import com.github.unchama.seichiassist.concurrent.PluginExecutionContexts.asyncShift
    val dynamicPartComputation = (for {
      homeNumber <- 1 to Home.maxHomePerPlayer
    } yield {
      val column = refineV[Interval.ClosedOpen[0, 9]](homeNumber - 1)
      implicit val ioCanReadHome: HomeReadAPI[IO] = environment.ioCanReadHome
      column match {
        case Right(value) => ChestSlotRef(1, value) -> setHomeNameButton[IO](homeNumber)
        case Left(_)      => throw new RuntimeException("This branch should not be reached.")
      }
    }.sequence).toList.sequence

    for {
      dynamicPart <- dynamicPartComputation
    } yield MenuSlotLayout(homePointPart.flatten ++ dynamicPart.toMap: _*)
  }

  private object ConstantButtons {
    def warpToHomePointButton(homeNumber: Int): Button =
      Button(
        new IconItemStackBuilder(Material.COMPASS)
          .title(s"$YELLOW$UNDERLINE${BOLD}ホームポイント${homeNumber}にワープ")
          .lore(
            List(
              s"${GRAY}あらかじめ設定した",
              s"${GRAY}ホームポイント${homeNumber}にワープします",
              s"${DARK_GRAY}うまく機能しない時は",
              s"${DARK_GRAY}再接続してみてください",
              s"$DARK_RED${UNDERLINE}クリックでワープ",
              s"${DARK_GRAY}command->[/home warp $homeNumber]"
            )
          )
          .build(),
        LeftClickButtonEffect {
          SequentialEffect(
            FocusedSoundEffect(Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1f, 1f),
            CommandEffect(s"home warp $homeNumber")
          )
        }
      )

    def setHomeButton(homeNumber: Int)(implicit environment: Environment): Button =
      Button(
        new IconItemStackBuilder(Material.BED)
          .title(s"$YELLOW$UNDERLINE${BOLD}ホームポイント${homeNumber}を設定")
          .lore(
            List(
              s"${GRAY}現在位置をホームポイント$homeNumber",
              s"${GRAY}として設定します",
              s"$DARK_GRAY※確認メニューが開きます",
              s"$DARK_RED${UNDERLINE}クリックで設定",
              s"${DARK_GRAY}command->[/home set $homeNumber]"
            )
          )
          .build(),
        LeftClickButtonEffect {
          SequentialEffect(
            FocusedSoundEffect(Sound.BLOCK_FENCE_GATE_OPEN, 1f, 0.1f),
            environment.ioCanOpenConfirmationMenu.open(HomeChangeConfirmationMenu(homeNumber))
          )
        }
      )

    def removeHomeButton(homeNumber: Int)(implicit environment: Environment): Button =
      Button(
        new IconItemStackBuilder(Material.WOOL, 14)
          .title(s"$RED$UNDERLINE${BOLD}ホームポイント${homeNumber}を削除")
          .lore(
            List(
              s"${GRAY}ホームポイント${homeNumber}を削除します。",
              s"$DARK_GRAY※確認メニューが開きます。",
              s"$DARK_RED${UNDERLINE}クリックで設定"
            )
          )
          .build(),
        LeftClickButtonEffect {
          FocusedSoundEffect(Sound.BLOCK_ENDERCHEST_CLOSE, 1f, 0.1f)
          SequentialEffect(
            environment
              .ioCanOpenHomeRemoveConfirmationMenu
              .open(HomeRemoveConfirmationMenu(homeNumber))
          )
        }
      )
  }

  private case class ButtonComputations(player: Player) {
    def setHomeNameButton[F[_]: HomeReadAPI: ConcurrentEffect](homeNumber: Int): IO[Button] = {
      import cats.implicits._

      val homeId = HomeId(homeNumber)

      val program = for {
        homeOpt <- HomeReadAPI[F].get(player.getUniqueId, homeId)
      } yield {
        val lore = homeOpt match {
          case None => List(s"${GRAY}ホームポイント$homeId", s"${GRAY}ポイント未設定")
          case Some(Home(optionName, location)) =>
            val worldName = {
              ManagedWorld
                .fromName(location.worldName)
                .map(_.japaneseName)
                .getOrElse(location.worldName)
            }

            val nameStatus = optionName match {
              case Some(name) =>
                List(s"${GRAY}ホームポイント${homeId}は", s"$GRAY$name", s"${GRAY}と名付けられています")
              case None => List(s"${GRAY}ホームポイント${homeId}は", s"${GRAY}名前が未設定です")
            }

            val commandInfo = List(
              s"$DARK_RED${UNDERLINE}クリックで名称変更",
              s"${DARK_GRAY}command->[/home name $homeId]"
            )

            val coordinates = List(s"$GRAY$worldName x:${Math.floor(location.x)} y:${Math
                .floor(location.y)} z:${Math.floor(location.z)}")

            nameStatus ++ commandInfo ++ coordinates
        }

        Button(
          new IconItemStackBuilder(Material.PAPER)
            .title(s"$YELLOW$UNDERLINE${BOLD}ホームポイント${homeNumber}の情報")
            .lore(lore)
            .build(),
          LeftClickButtonEffect {
            SequentialEffect(
              FocusedSoundEffect(Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1f, 1f),
              CommandEffect(s"home name $homeNumber"),
              closeInventoryEffect
            )
          }
        )
      }

      import cats.effect.implicits._

      program.toIO
    }
  }

  case class HomeChangeConfirmationMenu(changeHomeNumber: Int, homeName: String = "")
      extends Menu {
    override type Environment = ConfirmationMenuEnvironment.Environment

    /**
     * メニューのサイズとタイトルに関する情報
     */
    override val frame: MenuFrame = MenuFrame(3.chestRows, s"$RED${BOLD}ホームポイントを変更しますか")

    /**
     * @return
     *   `player`からメニューの[[MenuSlotLayout]]を計算する[[IO]]
     */
    override def computeMenuLayout(
      player: Player
    )(implicit environment: Environment): IO[MenuSlotLayout] = {
      val baseSlotMap =
        Map(ChestSlotRef(1, 2) -> changeButton, ChestSlotRef(1, 6) -> cancelButton)
      val slotMap = baseSlotMap ++ Map(ChestSlotRef(0, 4) -> informationButton)
      IO.pure(MenuSlotLayout(slotMap))
    }

    val changeButton: Button =
      Button(
        new IconItemStackBuilder(Material.WOOL, durability = 5).title(s"${GREEN}変更する").build(),
        LeftClickButtonEffect {
          SequentialEffect(
            FocusedSoundEffect(Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1f, 1f),
            CommandEffect(s"home set $changeHomeNumber"),
            closeInventoryEffect
          )
        }
      )

    def cancelButton(implicit environment: Environment): Button =
      Button(
        new IconItemStackBuilder(Material.WOOL, durability = 14).title(s"${RED}変更しない").build(),
        LeftClickButtonEffect {
          FocusedSoundEffect(Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1f, 1f)
          environment.ioCanOpenHomeMenu.open(HomeMenu)
        }
      )

    val informationButton: Button =
      Button(
        new IconItemStackBuilder(Material.PAPER)
          .title(s"${GREEN}設定するホームポイントの情報")
          .lore(List(s"${GRAY}No.$changeHomeNumber", s"${GRAY}名称：$homeName"))
          .build()
      )
  }

  object ConfirmationMenuEnvironment {

    class Environment(implicit val ioCanOpenHomeMenu: IO CanOpen HomeMenu.type)

  }

  case class HomeRemoveConfirmationMenu(removeHomeNumber: Int, homeName: String = "")
      extends Menu {

    /**
     * メニューを開く操作に必要な環境情報の型。 例えば、メニューが利用するAPIなどをここを通して渡すことができる。
     */
    override type Environment = ConfirmationMenuEnvironment.Environment

    /**
     * メニューのサイズとタイトルに関する情報
     */
    override val frame: MenuFrame = MenuFrame(3.chestRows, s"$RED${BOLD}ホームポイントを削除しますか")

    /**
     * @return
     * `player`からメニューの[[MenuSlotLayout]]を計算する[[IO]]
     */
    override def computeMenuLayout(
      player: Player
    )(implicit environment: Environment): IO[MenuSlotLayout] = {
      val baseSlotMap =
        Map(ChestSlotRef(1, 2) -> removeButton, ChestSlotRef(1, 6) -> cancelButton)
      val slotMap = baseSlotMap ++ Map(ChestSlotRef(0, 4) -> informationButton)
      IO.pure(MenuSlotLayout(slotMap))
    }

    val removeButton: Button =
      Button(
        new IconItemStackBuilder(Material.WOOL, durability = 5).title(s"${GREEN}削除する").build(),
        LeftClickButtonEffect {
          SequentialEffect(
            FocusedSoundEffect(Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1f, 1f),
            CommandEffect(s"home remove $removeHomeNumber"),
            closeInventoryEffect
          )
        }
      )

    def cancelButton(implicit environment: Environment): Button =
      Button(
        new IconItemStackBuilder(Material.WOOL, durability = 14).title(s"${RED}変更しない").build(),
        LeftClickButtonEffect {
          FocusedSoundEffect(Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1f, 1f)
          environment.ioCanOpenHomeMenu.open(HomeMenu)
        }
      )

    val informationButton: Button =
      Button(
        new IconItemStackBuilder(Material.PAPER)
          .title(s"${GREEN}設定するホームポイントの情報")
          .lore(List(s"${GRAY}No.$removeHomeNumber", s"${GRAY}名称：$homeName"))
          .build()
      )

  }

}
