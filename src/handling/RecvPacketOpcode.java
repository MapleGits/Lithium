/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

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
package handling;

import constants.GameConstants;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public enum RecvPacketOpcode implements WritableIntValueHolder {
    // GENERIC

    PONG(false),
    CLIENT_HELLO(false),
    // LOGIN
    LOGIN_PASSWORD(false),
    SEND_ENCRYPTED(false),
    CLIENT_ERROR(false),
    SERVERLIST_REQUEST,
    REDISPLAY_SERVERLIST,
    CHARLIST_REQUEST,
    SERVERSTATUS_REQUEST,
    CHECK_CHAR_NAME,
    UPDATE_ENV,
    CREATE_CHAR,
    DELETE_CHAR,
    STRANGE_DATA,
    CHAR_SELECT,
    AUTH_SECOND_PASSWORD,
    VIEW_ALL_CHAR,
    VIEW_REGISTER_PIC,
    ENABLE_SPECIAL_CREATION,
    CREATE_SPECIAL_CHAR,
    MONSTER_BOOK_DROPS,
    VIEW_SELECT_PIC,
    PICK_ALL_CHAR,
    TWIN_DRAGON_EGG,
    
    XMAS_SURPRISE,
    VICIOUS_HAMMER,
    USE_ALIEN_SOCKET,
    MAGIC_WHEEL,
    USE_ALIEN_SOCKET_RESPONSE,
    USE_NEBULITE_FUSION,
    CHAR_SELECT_NO_PIC,
    VIEW_SERVERLIST,
    RSA_KEY(false),
    CLIENT_START(false),
    CLIENT_FAILED(false),
    // CHANNEL
    PLAYER_LOGGEDIN(false),
    CHANGE_MAP,
    CHANGE_CHANNEL,
    CHANGE_ROOM_CHANNEL,
    ENTER_CASH_SHOP,
    MOVE_PLAYER,
    CANCEL_CHAIR,
    USE_TITLE,
    USE_CHAIR,
    CLOSE_RANGE_ATTACK,
    RANGED_ATTACK,
    MAGIC_ATTACK,
    PASSIVE_ENERGY,
    TAKE_DAMAGE,
    GENERAL_CHAT,
    CLOSE_CHALKBOARD,
    USE_NEBULITE,
    FACE_EXPRESSION,
    USE_ITEMEFFECT,
    WHEEL_OF_FORTUNE,
    NPC_TALK,
    NPC_TALK_MORE,
    NPC_SHOP,
    STORAGE,
    USE_HIRED_MERCHANT,
    MERCH_ITEM_STORE,
    DUEY_ACTION,
    ITEM_SORT,
    ITEM_GATHER,
    ITEM_MOVE,
    ITEM_MAKER,
    USE_ITEM,
    CANCEL_ITEM_EFFECT,
    //USE_FISHING, // Some unknown value sent by client after fishing for 30 sec, ignored
    USE_SUMMON_BAG,
    PET_FOOD,
    USE_MOUNT_FOOD,
    USE_SCRIPTED_NPC_ITEM,
    USE_CASH_ITEM,
    USE_CATCH_ITEM,
    USE_SKILL_BOOK,
    USE_RETURN_SCROLL,
    USE_UPGRADE_SCROLL,
    DISTRIBUTE_AP,
    AUTO_ASSIGN_AP,
    HEAL_OVER_TIME,
    DISTRIBUTE_SP,
    SPECIAL_MOVE,
    CANCEL_BUFF,
    SKILL_EFFECT,
    MESO_DROP,
    GIVE_FAME,
    CHAR_INFO_REQUEST,
    SPAWN_PET,
    CANCEL_DEBUFF,
    CHANGE_MAP_SPECIAL,
    USE_INNER_PORTAL,
    TROCK_ADD_MAP,
    QUEST_ACTION,
    SKILL_MACRO,
    REWARD_ITEM,
    USE_TREASUER_CHEST,
    PARTYCHAT,
    WHISPER,
    MESSENGER,
    PLAYER_INTERACTION,
    PARTY_OPERATION,
    DENY_PARTY_REQUEST,
    GUILD_OPERATION,
    DENY_GUILD_REQUEST,
    BUDDYLIST_MODIFY,
    NOTE_ACTION,
    USE_DOOR,
    CHANGE_KEYMAP,
    ENTER_MTS,
    ALLIANCE_OPERATION,
    DENY_ALLIANCE_REQUEST,
    REQUEST_FAMILY,
    OPEN_FAMILY,
    FAMILY_OPERATION,
    DELETE_JUNIOR,
    DELETE_SENIOR,
    ACCEPT_FAMILY,
    USE_FAMILY,
    FAMILY_PRECEPT,
    FAMILY_SUMMON,
    CYGNUS_SUMMON,
    ARAN_COMBO,
    BBS_OPERATION,
    TRANSFORM_PLAYER,
    MOVE_PET,
    PET_CHAT,
    PET_COMMAND,
    PET_LOOT,
    PET_AUTO_POT,
    MOVE_SUMMON,
    SUMMON_ATTACK,
    DAMAGE_SUMMON,
    MOVE_LIFE,
    AUTO_AGGRO,
    FRIENDLY_DAMAGE,
    MONSTER_BOMB,
    HYPNOTIZE_DMG,
    NPC_ACTION,
    ITEM_PICKUP,
    DAMAGE_REACTOR,
    SNOWBALL,
    LEFT_KNOCK_BACK,
    COCONUT,
    MONSTER_CARNIVAL,
    SHIP_OBJECT,
    CS_UPDATE,
    BUY_CS_ITEM,
    COUPON_CODE,
    MAPLETV,
    MOVE_DRAGON,
    REPAIR,
    REPAIR_ALL,
    TOUCHING_MTS,
    USE_MAGNIFY_GLASS,
    USE_POTENTIAL_SCROLL,
    USE_EQUIP_SCROLL,
    GAME_POLL,
    OWL,
    OWL_WARP,
    //XMAS_SURPRISE, //header -> uniqueid(long) is entire structure
    USE_OWL_MINERVA,
    RPS_GAME,
    UPDATE_QUEST,
    //QUEST_ITEM, //header -> questid(int) -> 1/0(byte, open or close)
    USE_ITEM_QUEST,
    FOLLOW_REQUEST,
    FOLLOW_REPLY,
    MOB_NODE,
    DISPLAY_NODE,
    TOUCH_REACTOR,
    RING_ACTION,
    SOLOMON,
    GACH_EXP,
    EXPEDITION_OPERATION,
    EXPEDITION_LISTING,
    PARTY_SEARCH_START,
    PARTY_SEARCH_STOP,
    USE_TELE_ROCK,
    SUB_SUMMON,
    USE_MECH_DOOR,
    MECH_CANCEL,
    REMOVE_SUMMON,
    AUTO_FOLLOW_REPLY,
    REPORT,
    MOB_BOMB,
    CREATE_ULTIMATE,
    PAM_SONG,
    USE_POT,
    CLEAR_POT,
    FEED_POT,
    CURE_POT,
    CRAFT_MAKE,
    CRAFT_DONE,
    CRAFT_EFFECT,
    STOP_HARVEST,
    START_HARVEST,
    MOVE_BAG,
    USE_BAG,
    CHANGE_SET,
    GET_BOOK_INFO,
    MOVE_ANDROID,
    FACE_ANDROID,
    REISSUE_MEDAL,
    CLICK_REACTOR,
    USE_RECIPE,
    USE_FAMILIAR,
    SPAWN_FAMILIAR,
    RENAME_FAMILIAR,
    MOVE_FAMILIAR,
    TOUCH_FAMILIAR,
    ATTACK_FAMILIAR,
    SIDEKICK_OPERATION,
    DENY_SIDEKICK_REQUEST,
    ALLOW_PARTY_INVITE,
    PROFESSION_INFO,
    QUICK_SLOT,
    MAKE_EXTRACTOR,
    USE_COSMETIC,
    USE_FLAG_SCROLL,
    SWITCH_BAG,
    REWARD_POT,
    PVP_INFO,
    ENTER_PVP,
    ENTER_PVP_PARTY,
    LEAVE_PVP,
    PVP_RESPAWN,
    PVP_ATTACK,
    PVP_SUMMON,
    PUBLIC_NPC,
    MTS_TAB;
    private short code = -2;

    @Override
    public void setValue(short code) {
        this.code = code;
    }

    @Override
    public final short getValue() {
        return code;
    }
    private boolean CheckState;

    private RecvPacketOpcode() {
        this.CheckState = true;
    }

    private RecvPacketOpcode(final boolean CheckState) {
        this.CheckState = CheckState;
    }

    public final boolean NeedsChecking() {
        return CheckState;
    }

    public static Properties getDefaultProperties() throws FileNotFoundException, IOException {
        Properties props = new Properties();
        FileInputStream fileInputStream = new FileInputStream(GameConstants.GMS ? "recvopsGMS.properties" : "recvops.properties");
        props.load(fileInputStream);
        fileInputStream.close();
        return props;
    }

    static {
        reloadValues();
    }

    public static final void reloadValues() {
        try {
            ExternalCodeTableGetter.populateValues(getDefaultProperties(), values());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load recvops", e);
        }
    }
}
