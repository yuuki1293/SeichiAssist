package com.github.unchama.seichiassist.commands;

import com.github.unchama.seichiassist.util.Util;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class StickCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		//プレイヤーを取得
		//プレイヤーネーム
		//String name = Util.getName(player);
		//UUIDを取得
		//UUID uuid = player.getUniqueId();
		//playerdataを取得
		//PlayerData playerdata = SeichiAssist.playermap.get(uuid);
		//プレイヤーからの送信でない時処理終了
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.GREEN + "このコマンドはゲーム内から実行してください。");
			return true;
		}else if(args.length == 0){
			//コマンド長が０の時の処理
			Player player = (Player)sender;
			ItemStack itemstack = new ItemStack(Material.STICK,1);
			itemstack.setAmount(1); //念のため追加
			if(!Util.isPlayerInventoryFull(player)){
				Util.addItem(player,itemstack);
				player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, (float)0.1, (float)1);
			}else{
				Util.dropItem(player,itemstack);
			}
			return true;
		}
		return false;
	}
}
