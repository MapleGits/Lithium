/*
This file is part of the ZeroFusion MapleStory Server
Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>
ZeroFusion organized by "RMZero213" <RMZero213@hotmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License version 3
as published by the Free Software Foundation. You may not use, modify
or distribute this program under any other version of the
GNU Affero General Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server;

import constants.GameConstants;
import client.inventory.Item;
import client.inventory.ItemLoader;
import client.inventory.MapleInventoryType;
import java.sql.Connection;
import database.DatabaseConnection;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import tools.Pair;

public class MTSCart implements Serializable {

    private static final long serialVersionUID = 231541893513373578L;
    private int characterId, tab = 1, type = 0, page = 0;
    //tab; 1 = buy now, 2 = wanted, 3 = auction, 4 = cart
    //type = inventorytype; 0 = anything
    //page = whatever
    private List<Item> transfer = new ArrayList<Item>();
    private List<Integer> cart = new ArrayList<Integer>();
    private List<Integer> notYetSold = new ArrayList<Integer>(10);
    private List<Integer> currentViewingItems = new ArrayList<Integer>();
    private int owedNX = 0;

    public MTSCart(int characterId) throws SQLException {
        this.characterId = characterId;
        for (Pair<Item, MapleInventoryType> item : ItemLoader.MTS_TRANSFER.loadItems(false, characterId).values()) {
            transfer.add(item.getLeft());
        }
        loadCart();
        loadNotYetSold();
    }

    public List<Item> getInventory() {
        return transfer;
    }

    public void addToInventory(Item item) {
        transfer.add(item);
    }

    public void removeFromInventory(Item item) {
        transfer.remove(item);
    }

    public List<Integer> getCart() {
        return cart;
    }

    public boolean addToCart(final int car) {
        if (!cart.contains(car)) {
            cart.add(car);
            return true;
        }
        return false;
    }

    public void removeFromCart(final int car) {
        for (int i = 0; i < cart.size(); i++) {
            if (cart.get(i) == car) {
                cart.remove(i);
            }
        }
    }

    public List<Integer> getNotYetSold() {
        return notYetSold;
    }

    public void addToNotYetSold(final int car) {
        notYetSold.add(car);
    }

    public void removeFromNotYetSold(final int car) {
        for (int i = 0; i < notYetSold.size(); i++) {
            if (notYetSold.get(i) == car) {
                notYetSold.remove(i);
            }
        }
    }

    public final int getSetOwedNX() {
        final int on = owedNX;
        owedNX = 0;
        return on;
    }

    public void increaseOwedNX(final int newNX) {
        owedNX += newNX;
    }

    public void save() throws SQLException {
        List<Pair<Item, MapleInventoryType>> itemsWithType = new ArrayList<Pair<Item, MapleInventoryType>>();

        for (Item item : getInventory()) {
            itemsWithType.add(new Pair<Item, MapleInventoryType>(item, GameConstants.getInventoryType(item.getItemId())));
        }

        ItemLoader.MTS_TRANSFER.saveItems(itemsWithType, characterId);
        final Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("DELETE FROM mts_cart WHERE characterid = ?");
        ps.setInt(1, characterId);
        ps.execute();
        ps.close();
        ps = con.prepareStatement("INSERT INTO mts_cart VALUES(DEFAULT, ?, ?)");
        ps.setInt(1, characterId);
        for (int i : cart) {
            ps.setInt(2, i);
            ps.executeUpdate();
        }
        if (owedNX > 0) {
            ps.setInt(2, -owedNX);
            ps.executeUpdate();
        }
        ps.close();
        //notYetSold shouldnt be saved here
    }

    public void loadCart() throws SQLException {
        final PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM mts_cart WHERE characterid = ?");
        ps.setInt(1, characterId);
        final ResultSet rs = ps.executeQuery();
        int iId;
        while (rs.next()) {
            iId = rs.getInt("itemid");
            if (iId < 0) {
                owedNX -= iId;
            } else if (MTSStorage.getInstance().check(iId)) {
                cart.add(iId);
            }
        }
        rs.close();
        ps.close();
    }

    public void loadNotYetSold() throws SQLException {
        final PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM mts_items WHERE characterid = ?");
        ps.setInt(1, characterId);
        final ResultSet rs = ps.executeQuery();
        int pId;
        while (rs.next()) {
            pId = rs.getInt("id");
            if (MTSStorage.getInstance().check(pId)) {
                notYetSold.add(pId);
            }
        }
        rs.close();
        ps.close();
    }

    public void changeInfo(final int tab, final int type, final int page) {
	if (tab != this.tab || type != this.type) { //changed
	    refreshCurrentView(tab, type);
	}
        this.tab = tab;
        this.type = type;
        this.page = page;
    }

    public int getTab() {
        return tab;
    }

    public int getType() {
        return type;
    }

    public int getPage() {
        return page;
    }

    public List<Integer> getCurrentViewPage() {
	final List<Integer> ret = new ArrayList<Integer>();
	final int size = currentViewingItems.size() / 16 + (currentViewingItems.size() % 16 > 0 ? 1 : 0);
	if (page > size) {
	    page = 0;
	}
	for (int i = page * 16; i < page * 16 + 16; i++) {
	    if (currentViewingItems.size() > i) {
		ret.add(currentViewingItems.get(i));
	    } else {
		break;
	    }
	}
	return ret;
    }

    public List<Integer> getCurrentView() {
	return currentViewingItems;
    }


    public void refreshCurrentView() {
	refreshCurrentView(tab, type);
    }

    public void refreshCurrentView(final int newTab, final int newType) {
	currentViewingItems.clear();
	if (newTab == 1) {
	    currentViewingItems = MTSStorage.getInstance().getBuyNow(newType);
	} else if (newTab == 4) {
	    for (int i : cart) {
		if (newType == 0 || (GameConstants.getInventoryType(i).getType() == newType)) {
		    currentViewingItems.add(i);
		}
	    }
	}
    }

    public void changeCurrentView(List<Integer> items) {
	currentViewingItems.clear();
	currentViewingItems = items;
    }
}
