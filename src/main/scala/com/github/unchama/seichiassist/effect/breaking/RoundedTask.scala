package com.github.unchama.seichiassist.effect.breaking

import org.bukkit.scheduler.BukkitRunnable
abstract class RoundedTask extends BukkitRunnable() {
  private var round = 0

  def firstAction(): Unit

  def secondAction(): Unit

  def otherwiseAction(): Unit = {
    cancel()
  }

  override def run(): Unit = {
    round += 1
    round match {
      case 1 => firstAction()
      case 2 => secondAction()
      case _ => otherwiseAction()
    }
  }
}