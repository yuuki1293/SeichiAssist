package com.github.unchama.seichiassist.data;

import com.github.unchama.seichiassist.SeichiAssist;
import com.github.unchama.seichiassist.util.Util;
import com.github.unchama.seichiassist.util.Util.ChuckType;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * 保護関連メニュー
 *
 * @author karayuu
 */
public class RegionMenuData {
    static WorldGuardPlugin Wg = Util.getWorldGuard();
    static WorldEditPlugin We = Util.getWorldEdit();

    /**
     * 保護メニューを取得します。
     * @param player プレイヤー
     * @return 保護メニューインベントリ
     */
    public static Inventory getRegionMenuData(Player player) {
        Inventory inventory = Bukkit.createInventory(null, InventoryType.HOPPER, ChatColor.BLACK + "保護メニュー");

        //0マス目
        List<String> lore0 = Arrays.asList(ChatColor.RESET + "" +  ChatColor.DARK_RED + "" + ChatColor.UNDERLINE + "クリックで召喚"
                , ChatColor.RESET + "" + ChatColor.DARK_GRAY + "※インベントリを空けておこう"
                , ChatColor.RESET + "" +  ChatColor.DARK_GREEN + "" + ChatColor.UNDERLINE + "保護のかけ方"
                , ChatColor.RESET + "" +  ChatColor.GREEN + "①召喚された斧を手に持ちます"
                , ChatColor.RESET + "" +  ChatColor.GREEN + "②保護したい領域の一方の角を" + ChatColor.YELLOW + "左" + ChatColor.GREEN + "クリック"
                , ChatColor.RESET + "" +  ChatColor.GREEN + "③もう一方の対角線上の角を" + ChatColor.RED + "右" + ChatColor.GREEN + "クリック"
                , ChatColor.RESET + "" +  ChatColor.GREEN + "④メニューの" + ChatColor.RESET + "" +  ChatColor.YELLOW + "金の斧" + ChatColor.RESET + "" +  ChatColor.GREEN + "をクリック"
                , ChatColor.RESET + "" + ChatColor.DARK_GRAY + "command->[//wand]");
        ItemStack menuicon0 = Util.getMenuIcon(Material.WOOD_AXE, 1
                , ChatColor.YELLOW + "" + ChatColor.UNDERLINE + "" + ChatColor.BOLD + "保護設定用の木の斧を召喚"
                , lore0, true);
        inventory.setItem(0, menuicon0);

        //1マス目
        ItemStack itemstack1 = new ItemStack(Material.GOLD_AXE,1);
        ItemMeta itemmeta1 = Bukkit.getItemFactory().getItemMeta(Material.GOLD_AXE);
        itemmeta1.setDisplayName(ChatColor.YELLOW + "" + ChatColor.UNDERLINE + "" + ChatColor.BOLD + "保護の申請");
        itemmeta1.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        List<String> lore1 = new ArrayList<>();

        Selection selection = Util.getWorldEdit().getSelection(player);

        if(!player.hasPermission("worldguard.region.claim")){
            lore1.addAll(Arrays.asList(ChatColor.RED + "このワールドでは"
                    , ChatColor.RED + "保護を申請出来ません"
            ));
        }else if (selection == null) {
            lore1.addAll(Arrays.asList(ChatColor.RED + "範囲指定されてません"
                    , ChatColor.RED + "先に木の斧で2か所クリックしてネ"
            ));
        }else if(selection.getLength() < 10||selection.getWidth() < 10){
            lore1.addAll(Arrays.asList(ChatColor.RED + "選択された範囲が狭すぎます"
                    , ChatColor.RED + "1辺当たり最低10ブロック以上にしてネ"
            ));
        }else{
            itemmeta1.addEnchant(Enchantment.DIG_SPEED, 100, false);
            lore1.addAll(Arrays.asList(ChatColor.DARK_GREEN + "" + ChatColor.UNDERLINE + "範囲指定されています"
                    , ChatColor.DARK_GREEN + "" + ChatColor.UNDERLINE + "クリックすると保護を申請します"
            ));
        }

        if(player.hasPermission("worldguard.region.claim")){
            PlayerData playerdata = SeichiAssist.playermap.get(player.getUniqueId());
            lore1.addAll(Arrays.asList(ChatColor.DARK_GRAY + "Y座標は自動で全範囲保護されます"
                    , ChatColor.RESET + "" + ChatColor.YELLOW + "" + "A new region has been claimed"
                    , ChatColor.RESET + "" + ChatColor.YELLOW + "" + "named '" + player.getName() + "_" + playerdata.rgnum + "'."
                    , ChatColor.RESET + "" + ChatColor.GRAY + "と出れば保護設定完了です"
                    , ChatColor.RESET + "" + ChatColor.RED + "赤色で別の英文が出た場合"
                    , ChatColor.RESET + "" + ChatColor.GRAY + "保護の設定に失敗しています"
                    , ChatColor.RESET + "" + ChatColor.GRAY + "・別の保護と被っていないか"
                    , ChatColor.RESET + "" + ChatColor.GRAY + "・保護数上限に達していないか"
                    , ChatColor.RESET + "" + ChatColor.GRAY + "確認してください"
            ));
        }
        itemmeta1.setLore(lore1);
        itemstack1.setItemMeta(itemmeta1);
        inventory.setItem(1,itemstack1);

        //2マス目
        List<String> lore2 =  Arrays.asList(ChatColor.RESET + "" +  ChatColor.DARK_RED + "" + ChatColor.UNDERLINE + "クリックで表示"
                , ChatColor.RESET + "" + ChatColor.GRAY + "今いるワールドで"
                , ChatColor.RESET + "" + ChatColor.GRAY + "あなたが保護している"
                , ChatColor.RESET + "" + ChatColor.GRAY + "土地の一覧を表示します"
                , ChatColor.RESET + "" + ChatColor.RED + "" + ChatColor.UNDERLINE + "/rg info 保護名"
                , ChatColor.RESET + "" + ChatColor.GRAY + "該当保護の詳細情報を表示"
                , ChatColor.RESET + "" + ChatColor.RED + "" + ChatColor.UNDERLINE + "/rg rem 保護名"
                , ChatColor.RESET + "" + ChatColor.GRAY + "該当保護を削除する"
                , ChatColor.RESET + "" + ChatColor.RED + "" + ChatColor.UNDERLINE + "/rg addmem 保護名 プレイヤー名"
                , ChatColor.RESET + "" + ChatColor.GRAY + "該当保護に指定メンバーを追加"
                , ChatColor.RESET + "" + ChatColor.RED + "" + ChatColor.UNDERLINE + "/rg removemember 保護名 プレイヤー名"
                , ChatColor.RESET + "" + ChatColor.GRAY + "該当保護の指定メンバーを削除"
                , ChatColor.RESET + "" + ChatColor.DARK_GRAY + "その他のコマンドはWikiを参照"
                , ChatColor.RESET + "" + ChatColor.DARK_GRAY + "command->[/rg list]"
        );
        ItemStack menuicon2 = Util.getMenuIcon(Material.STONE_AXE, 1,
                ChatColor.YELLOW + "" + ChatColor.UNDERLINE + "" + ChatColor.BOLD + "保護一覧を表示", lore2, true);
        inventory.setItem(2, menuicon2);

        //3マス目
        List<String> lore3 = Arrays.asList(ChatColor.RESET + "" +  ChatColor.DARK_RED + "" + ChatColor.UNDERLINE + "クリックで開く"
                , ChatColor.RESET + "" + ChatColor.RED + "保護の作成と管理が超簡単に！"
                , ChatColor.RESET + "" + ChatColor.RED + "クリックした場所によって挙動が変わります"
                , ChatColor.RESET + "" + ChatColor.YELLOW + "自分の所有する保護内なら…"
                , ChatColor.RESET + "" + ChatColor.GRAY + "保護の各種設定や削除が行えます"
                , ChatColor.RESET + "" + ChatColor.YELLOW + "それ以外なら…"
                , ChatColor.RESET + "" + ChatColor.GRAY + "新規保護の作成画面が表示されます"
                , ChatColor.RESET + "" + ChatColor.DARK_GRAY + "command->[/land]"
                );
        ItemStack menuicon3 = Util.getMenuIcon(Material.DIAMOND_AXE, 1,
                ChatColor.YELLOW + "" + ChatColor.UNDERLINE + "" + ChatColor.BOLD + "RegionGUI機能", lore3, true);
        inventory.setItem(3, menuicon3);

        //4マス目
        List<String> lore4 = Arrays.asList(ChatColor.RESET + "" + ChatColor.DARK_RED + "" + ChatColor.UNDERLINE + "クリックで開く"
                , ChatColor.RESET + "" + ChatColor.RED + "グリッド式保護の作成ができます"
                , ChatColor.RESET + "" + ChatColor.YELLOW + "グリッド式保護とは…"
                , ChatColor.RESET + "" + ChatColor.GRAY + "保護をチャンク単位で管理するシステムのこと"
                , ChatColor.RESET + "" + ChatColor.RED + "運営チームが個別に指定したワールドに関しては"
                , ChatColor.RESET + "" + ChatColor.RED + "このシステムのみでしか保護が作成できません"
        );
        ItemStack menuicon4 = Util.getMenuIcon(Material.IRON_AXE, 1,
                ChatColor.YELLOW + "" + ChatColor.UNDERLINE + "" + ChatColor.BOLD + "グリッド式保護作成画面",
                lore4, true);
        inventory.setItem(4, menuicon4);


        return inventory;
    }

    /**
     * グリッド式保護メニュを開きます。
     * @param player
     * @return
     */
    public static Inventory getGridWorldGuardMenu(Player player) {
        PlayerData playerData = SeichiAssist.playermap.get(player.getUniqueId());
        Map<ChuckType, Integer> chunkMap = playerData.getGridChuckMap();
        Map<ChuckType, String> directionMap = getPlayerDirectionString(player);

        Inventory gridInv = Bukkit.createInventory(null, InventoryType.DISPENSER,
                ChatColor.LIGHT_PURPLE + "グリッド式保護設定メニュー");

        //1マス目
        List<String> lore1 = getGridLore(directionMap.get(ChuckType.AHEAD), chunkMap.get(ChuckType.AHEAD));
        if (!playerData.canGridExtend(ChuckType.AHEAD)) {
            lore1.add(ChatColor.RED + "" + ChatColor.UNDERLINE + "これ以上拡張できません");
        } else if (!playerData.canGridReduce(ChuckType.AHEAD)) {
            lore1.add(ChatColor.RED + "" + ChatColor.UNDERLINE + "これ以上縮小できません");
        }
        ItemStack menuicon1 = Util.getMenuIcon(Material.STAINED_GLASS_PANE, 1, 14, ChatColor.DARK_GREEN + "前に1チャンク増やす/減らす",
                lore1, true);
        gridInv.setItem(1, menuicon1);

        //3マス目
        List<String> lore3 = getGridLore(directionMap.get(ChuckType.LEFT), chunkMap.get(ChuckType.LEFT));
        if (!playerData.canGridExtend(ChuckType.LEFT)) {
            lore3.add(ChatColor.RED + "" + ChatColor.UNDERLINE + "これ以上拡張できません");
        } else if (!playerData.canGridReduce(ChuckType.LEFT)) {
            lore3.add(ChatColor.RED + "" + ChatColor.UNDERLINE + "これ以上縮小できません");
        }
        ItemStack menuicon3 = Util.getMenuIcon(Material.STAINED_GLASS_PANE, 1, 10, ChatColor.DARK_GREEN + "左に1チャンク増やす/減らす",
                lore3, true);
        gridInv.setItem(3, menuicon3);

        //4マス目
        List<String> lore4 = new ArrayList<>();
        lore4.add(ChatColor.GRAY + "現在の設定");
        lore4.add(ChatColor.GRAY + "前方向：" + ChatColor.AQUA + chunkMap.get(ChuckType.AHEAD));
        lore4.add(ChatColor.GRAY + "後ろ方向：" + ChatColor.AQUA + chunkMap.get(ChuckType.BEHIND));
        lore4.add(ChatColor.GRAY + "右方向：" + ChatColor.AQUA + chunkMap.get(ChuckType.RIGHT));
        lore4.add(ChatColor.GRAY + "左方向：" + ChatColor.AQUA + chunkMap.get(ChuckType.LEFT));
        lore4.add(ChatColor.GRAY + "保護チャンク数：" + ChatColor.AQUA + playerData.getGridChunkAmount());
        ItemStack menuicon4 = Util.getMenuIcon(Material.STAINED_GLASS_PANE, 1, 11, ChatColor.DARK_GREEN + "設定",
                lore4, true);
        gridInv.setItem(4, menuicon4);

        //5マス目
        List<String> lore5 = getGridLore(directionMap.get(ChuckType.RIGHT), chunkMap.get(ChuckType.RIGHT));
        if (!playerData.canGridExtend(ChuckType.RIGHT)) {
            lore5.add(ChatColor.RED + "" + ChatColor.UNDERLINE + "これ以上拡張できません");
        } else if (!playerData.canGridReduce(ChuckType.RIGHT)) {
            lore5.add(ChatColor.RED + "" + ChatColor.UNDERLINE + "これ以上縮小できません");
        }
        ItemStack menuicon5 = Util.getMenuIcon(Material.STAINED_GLASS_PANE, 1, 5, ChatColor.DARK_GREEN + "右に1チャンク増やす/減らす",
                lore5, true);
        gridInv.setItem(5, menuicon5);

        //7マス目
        List<String> lore7 = getGridLore(directionMap.get(ChuckType.BEHIND), chunkMap.get(ChuckType.BEHIND));
        if (!playerData.canGridExtend(ChuckType.BEHIND)) {
            lore7.add(ChatColor.RED + "" + ChatColor.UNDERLINE + "これ以上拡張できません");
        } else if (!playerData.canGridReduce(ChuckType.BEHIND)) {
            lore7.add(ChatColor.RED + "" + ChatColor.UNDERLINE + "これ以上縮小できません");
        }
        ItemStack menuicon7 = Util.getMenuIcon(Material.STAINED_GLASS_PANE, 1, 13, ChatColor.DARK_GREEN + "後ろに1チャンク増やす/減らす",
                lore7, true);
        gridInv.setItem(7, menuicon7);

        //8マス目
        if (!player.hasPermission("worldguard.region.claim")) {
            List<String> lore8 = new ArrayList<>();
            lore8.add(ChatColor.RED + "" + ChatColor.UNDERLINE + "このワールドでは保護を作成できません");
            ItemStack menuicon8 = Util.getMenuIcon(Material.WOOL, 1, 14, ChatColor.RED + "保護作成",
                    lore8, true);
            gridInv.setItem(8, menuicon8);
        } else if (!playerData.canCreateRegion()) {
            List<String> lore8 = new ArrayList<>();
            lore8.add(ChatColor.RED + "" + ChatColor.UNDERLINE + "以下の原因により保護を作成できません");
            lore8.add(ChatColor.RED + "・保護の範囲が他の保護と重複している");
            lore8.add(ChatColor.RED + "・保護の作成上限に達している");
            ItemStack menuicon8 = Util.getMenuIcon(Material.WOOL, 1, 14, ChatColor.RED + "保護作成",
                    lore8, true);
            gridInv.setItem(8, menuicon8);
        } else {
            List<String> lore8 = new ArrayList<>();
            lore8.add(ChatColor.DARK_GREEN + "保護作成可能です");
            lore8.add(ChatColor.RED + "" + ChatColor.UNDERLINE + "クリックで作成");
            ItemStack menuicon8 = Util.getMenuIcon(Material.WOOL, 1, 11, ChatColor.GREEN + "保護作成",
                    lore8, true);
            gridInv.setItem(8, menuicon8);
        }
        return gridInv;
    }

    private static List<String> getGridLore(String direction, int chunk) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.RESET + "" +  ChatColor.GREEN + "左クリックで増加");
        lore.add(ChatColor.RESET + "" +  ChatColor.RED + "右クリックで減少");
        lore.add(ChatColor.RESET + "" + ChatColor.GRAY + ChatColor.GRAY + "---------------");
        lore.add(ChatColor.GRAY + "方向：" + ChatColor.AQUA + direction);
        lore.add(ChatColor.GRAY + "現在の指定方向チャンク数：" + ChatColor.AQUA + chunk);

        return lore;
    }

    private static Map<ChuckType, String> getPlayerDirectionString(Player player) {
        Float yaw = player.getLocation().getYaw();
        Map<ChuckType, String> directionMap = new HashMap<>();

        //0,360:south 90:west 180:north 270:east
        if (135 <= yaw && yaw < 225) {
            directionMap.put(ChuckType.BEHIND, "南(South)");
            directionMap.put(ChuckType.AHEAD, "北(North)");
            directionMap.put(ChuckType.LEFT, "西(West)");
            directionMap.put(ChuckType.RIGHT, "東(East)");
        } else if (225 <= yaw && yaw < 315) {
            directionMap.put(ChuckType.RIGHT, "南(South)");
            directionMap.put(ChuckType.LEFT, "北(North)");
            directionMap.put(ChuckType.BEHIND, "西(West)");
            directionMap.put(ChuckType.AHEAD, "東(East)");
        } else if (315 <= yaw || yaw < 45) {
            directionMap.put(ChuckType.AHEAD, "南(South)");
            directionMap.put(ChuckType.BEHIND, "北(North)");
            directionMap.put(ChuckType.RIGHT, "西(West)");
            directionMap.put(ChuckType.LEFT, "東(East)");
        } else if (45 <= yaw && yaw < 135) {
            directionMap.put(ChuckType.LEFT, "南(South)");
            directionMap.put(ChuckType.RIGHT, "北(North)");
            directionMap.put(ChuckType.AHEAD, "西(West)");
            directionMap.put(ChuckType.BEHIND, "東(East)");
        }
        return directionMap;
    }
}