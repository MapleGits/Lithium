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
package tools.packet;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import client.inventory.Equip;
import client.Skill;
import constants.GameConstants;
import client.inventory.MapleRing;
import client.inventory.MaplePet;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleCoolDownValueHolder;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.MapleQuestStatus;
import client.MapleTrait.MapleTraitType;
import client.inventory.Item;
import client.SkillEntry;
import handling.Buffstat;
import handling.world.MapleCharacterLook;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.SimpleTimeZone;
import server.MapleItemInformationProvider;
import server.MapleShop;
import server.MapleShopItem;
import server.Randomizer;
import tools.Pair;
import server.movement.LifeMovementFragment;
import server.quest.MapleQuest;
import server.shops.AbstractPlayerStore;
import server.shops.IMaplePlayerShop;
import tools.BitTools;
import tools.StringUtil;
import tools.Triple;
import tools.data.MaplePacketLittleEndianWriter;

public class PacketHelper {

    public final static long FT_UT_OFFSET = 116444592000000000L; // EDT
    public final static long MAX_TIME = 150842304000000000L; //00 80 05 BB 46 E6 17 02
    public final static long ZERO_TIME = 94354848000000000L; //00 40 E0 FD 3B 37 4F 01
    public final static long PERMANENT = 150841440000000000L; // 00 C0 9B 90 7D E5 17 02

    public static final long getKoreanTimestamp(final long realTimestamp) {
        return getTime(realTimestamp);
    }

    public static final long getTime(long realTimestamp) {
        if (realTimestamp == -1) {
            return MAX_TIME;
        } else if (realTimestamp == -2) {
            return ZERO_TIME;
        } else if (realTimestamp == -3) {
            return PERMANENT;
        }
        return ((realTimestamp * 10000) + FT_UT_OFFSET);
    }

    public static long getFileTimestamp(long timeStampinMillis, boolean roundToMinutes) {
        if (SimpleTimeZone.getDefault().inDaylightTime(new Date())) {
            timeStampinMillis -= 3600000L;
        }
        long time;
        if (roundToMinutes) {
            time = (timeStampinMillis / 1000 / 60) * 600000000;
        } else {
            time = timeStampinMillis * 10000;
        }
        return time + FT_UT_OFFSET;
    }

    public static void addQuestInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        final boolean idk = true;

        // 0x2000
        final List<MapleQuestStatus> started = chr.getStartedQuests();
        mplew.write(idk ? 1 : 0); // boolean
        if (idk) {
            mplew.writeShort(started.size());
            for (final MapleQuestStatus q : started) {
                mplew.writeShort(q.getQuest().getId());
                if (q.hasMobKills()) {
                    final StringBuilder sb = new StringBuilder();
                    for (final int kills : q.getMobKills().values()) {
                        sb.append(StringUtil.getLeftPaddedStr(String.valueOf(kills), '0', 3));
                    }
                    mplew.writeMapleAsciiString(sb.toString());
                } else {
                    mplew.writeMapleAsciiString(q.getCustomData() == null ? "" : q.getCustomData());
                }
            }

        } else {
            mplew.writeShort(0); // size, one short per size
        }
        mplew.writeShort(0); // size, two strings per size

        // 0x4000
        mplew.write(idk ? 1 : 0); //dunno
        if (idk) {
            final List<MapleQuestStatus> completed = chr.getCompletedQuests();
            mplew.writeShort(completed.size());
            for (final MapleQuestStatus q : completed) {
                mplew.writeShort(q.getQuest().getId());
                mplew.writeLong(getTime(q.getCompletionTime()));
            }
        } else {
            mplew.writeShort(0); // size, one short per size
        }
    }

    public static final void addSkillInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) { // 0x100
        final Map<Skill, SkillEntry> skills = chr.getSkills();
        boolean useOld = skills.size() < 500;
        mplew.write(useOld ? 1 : 0); // To handle the old skill system or something? 
        if (useOld) {
            mplew.writeShort(skills.size());
            for (final Entry<Skill, SkillEntry> skill : skills.entrySet()) {
                mplew.writeInt(skill.getKey().getId());
                mplew.writeInt(skill.getValue().skillevel);
                addExpirationTime(mplew, skill.getValue().expiration);

                if (skill.getKey().isFourthJob()) {
                    mplew.writeInt(skill.getValue().masterlevel);
                }
            }
        } else {
            final Map<Integer, Integer> skillsWithoutMax = new LinkedHashMap<>();
            final Map<Integer, Long> skillsWithExpiration = new LinkedHashMap<>();
            final Map<Integer, Byte> skillsWithMax = new LinkedHashMap<>();

            // Fill in these maps
            for (final Entry<Skill, SkillEntry> skill : skills.entrySet()) {
                skillsWithoutMax.put(skill.getKey().getId(), skill.getValue().skillevel);
                if (skill.getValue().expiration > 0) {
                    skillsWithExpiration.put(skill.getKey().getId(), skill.getValue().expiration);
                }
                if (skill.getKey().isFourthJob()) {
                    skillsWithMax.put(skill.getKey().getId(), skill.getValue().masterlevel);
                }
            }

            int amount = skillsWithoutMax.size();
            mplew.writeShort(amount);
            for (final Entry<Integer, Integer> x : skillsWithoutMax.entrySet()) {
                mplew.writeInt(x.getKey());
                mplew.writeInt(x.getValue()); // 80000000, 80000001, 80001040 show cid if linked.
            }
            mplew.writeShort(0); // For each, int

            amount = skillsWithExpiration.size();
            mplew.writeShort(amount);
            for (final Entry<Integer, Long> x : skillsWithExpiration.entrySet()) {
                mplew.writeInt(x.getKey());
                mplew.writeLong(x.getValue()); // Probably expiring skills here
            }
            mplew.writeShort(0); // For each, int

            amount = skillsWithMax.size();
            mplew.writeShort(amount);
            for (final Entry<Integer, Byte> x : skillsWithMax.entrySet()) {
                mplew.writeInt(x.getKey());
                mplew.writeInt(x.getValue());
            }
            mplew.writeShort(0); // For each, int (Master level = 0? O.O)
        }
    }

    public static final void addCoolDownInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        final List<MapleCoolDownValueHolder> cd = chr.getCooldowns();
        mplew.writeShort(cd.size());
        for (final MapleCoolDownValueHolder cooling : cd) {
            mplew.writeInt(cooling.skillId);
            mplew.writeShort((int) (cooling.length + cooling.startTime - System.currentTimeMillis()) / 1000);
        }
    }

    public static final void addRocksInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        final int[] mapz = chr.getRegRocks();
        for (int i = 0; i < 5; i++) { // VIP teleport map
            mplew.writeInt(mapz[i]);
        }

        final int[] map = chr.getRocks();
        for (int i = 0; i < 10; i++) { // VIP teleport map
            mplew.writeInt(map[i]);
        }

        final int[] maps = chr.getHyperRocks();
        for (int i = 0; i < 13; i++) { // VIP teleport map
            mplew.writeInt(maps[i]);
        }
        for (int i = 0; i < 13; i++) { // VIP teleport map
            mplew.writeInt(maps[i]);
        }
    }

    public static final void addRingInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        mplew.writeShort(0); // 0x400
        //01 00 = size
        //01 00 00 00 = gametype?
        //03 00 00 00 = win
        //00 00 00 00 = tie/loss
        //01 00 00 00 = tie/loss
        //16 08 00 00 = points

        // 0x800
        Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> aRing = chr.getRings(true);
        List<MapleRing> cRing = aRing.getLeft();
        mplew.writeShort(cRing.size());
        for (MapleRing ring : cRing) { // 33
            mplew.writeInt(ring.getPartnerChrId());
            mplew.writeAsciiString(ring.getPartnerName(), 13);
            mplew.writeLong(ring.getRingId());
            mplew.writeLong(ring.getPartnerRingId());
        }
        List<MapleRing> fRing = aRing.getMid();
        mplew.writeShort(fRing.size());
        for (MapleRing ring : fRing) { // 37
            mplew.writeInt(ring.getPartnerChrId());
            mplew.writeAsciiString(ring.getPartnerName(), 13);
            mplew.writeLong(ring.getRingId());
            mplew.writeLong(ring.getPartnerRingId());
            mplew.writeInt(ring.getItemId());
        }
        List<MapleRing> mRing = aRing.getRight();
        mplew.writeShort(mRing.size());
        int marriageId = 30000;
        for (MapleRing ring : mRing) { // 48
            mplew.writeInt(marriageId);
            mplew.writeInt(chr.getId());
            mplew.writeInt(ring.getPartnerChrId());
            mplew.writeShort(3); //1 = engaged 3 = married
            mplew.writeInt(ring.getItemId());
            mplew.writeInt(ring.getItemId());
            mplew.writeAsciiString(chr.getName(), 13);
            mplew.writeAsciiString(ring.getPartnerName(), 13);
        }
    }

    public static void addInventoryInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeInt(chr.getMeso()); // mesos
        mplew.writeInt(0); // 4 ints per size
        mplew.write(chr.getInventory(MapleInventoryType.EQUIP).getSlotLimit()); // equip slots
        mplew.write(chr.getInventory(MapleInventoryType.USE).getSlotLimit()); // use slots
        mplew.write(chr.getInventory(MapleInventoryType.SETUP).getSlotLimit()); // set-up slots
        mplew.write(chr.getInventory(MapleInventoryType.ETC).getSlotLimit()); // etc slots
        mplew.write(chr.getInventory(MapleInventoryType.CASH).getSlotLimit()); // cash slots

        final MapleQuestStatus stat = chr.getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT)); // 0x200000 : int + int actually
        if (stat != null && stat.getCustomData() != null && Long.parseLong(stat.getCustomData()) > System.currentTimeMillis()) {
            mplew.writeLong(getTime(Long.parseLong(stat.getCustomData())));
        } else {
            mplew.writeLong(getTime(-2));
        }
        MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED);
        List<Item> equipped = iv.newList();
        Collections.sort(equipped);
        for (Item item : equipped) {
            if (item.getPosition() < 0 && item.getPosition() > -100) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.writeShort(0); // start of equipped nx
        for (Item item : equipped) {
            if (item.getPosition() <= -100 && item.getPosition() > -1000) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.writeShort(0); // start of equip inventory
        iv = chr.getInventory(MapleInventoryType.EQUIP);
        for (Item item : iv.list()) {
            addItemPosition(mplew, item, false, false);
            addItemInfo(mplew, item, chr);
        }
        mplew.writeShort(0); //start of evan equips
        for (Item item : equipped) {
            if (item.getPosition() <= -1000 && item.getPosition() > -1100) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.writeShort(0); //start of mechanic equips, ty KDMS
        for (Item item : equipped) {
            if (item.getPosition() <= -1100 && item.getPosition() > -1200) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.writeShort(0); // start of android equips
        for (Item item : equipped) {
            if (item.getPosition() <= -1200) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.writeShort(0); // start of use inventory
        iv = chr.getInventory(MapleInventoryType.USE);
        for (Item item : iv.list()) {
            addItemPosition(mplew, item, false, false);
            addItemInfo(mplew, item, chr);
        }
        mplew.write(0); // start of set-up inventory
        iv = chr.getInventory(MapleInventoryType.SETUP);
        for (Item item : iv.list()) {
            addItemPosition(mplew, item, false, false);
            addItemInfo(mplew, item, chr);
        }
        mplew.write(0); // start of etc inventory
        iv = chr.getInventory(MapleInventoryType.ETC);
        for (Item item : iv.list()) {
            if (item.getPosition() < 100) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.write(0); // start of cash inventory
        iv = chr.getInventory(MapleInventoryType.CASH);
        for (Item item : iv.list()) {
            addItemPosition(mplew, item, false, false);
            addItemInfo(mplew, item, chr);
        }
        mplew.write(0); // start of extended slots
        for (int i = 0; i < chr.getExtendedSlots().size(); i++) {
            mplew.writeInt(i);
            mplew.writeInt(chr.getExtendedSlot(i));
            for (Item item : chr.getInventory(MapleInventoryType.ETC).list()) {
                if (item.getPosition() > (i * 100 + 100) && item.getPosition() < (i * 100 + 200)) {
                    addItemPosition(mplew, item, false, true);
                    addItemInfo(mplew, item, chr);
                }
            }
            mplew.writeInt(-1);
        }
        mplew.writeInt(-1);
        mplew.writeInt(0); // 0x40000000 Foreach : Int + Long
        mplew.writeInt(0); // 0x400 Foreach : Long + Long
        mplew.write(0); // 0x20000000 if got, then below
		/*mplew.writeInt(0);
        mplew.write(0);
        mplew.write(0);		
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.writeLong(0);
        
        mplew.write(0); // a boolean
         */
    }

    public static final void addCharStats(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        //63 B7 00 00 //alphaeta
        //41 6C 70 68 61 45 74 61 00 00 00 00 00 
        mplew.writeInt(chr.getId()); // character id
        mplew.writeAsciiString(chr.getName(), 13);
        mplew.write(chr.getGender()); // gender (0 = male, 1 = female)
        mplew.write(chr.getSkinColor()); // skin color
        mplew.writeInt(chr.getFace()); // face
        mplew.writeInt(chr.getHair()); // hair
        mplew.writeZeroBytes(24); // pets
        mplew.write(chr.getLevel()); // level
        mplew.writeShort(chr.getJob()); // job
        chr.getStat().connectData(mplew);
        mplew.writeShort(chr.getRemainingAp()); // remaining ap
        if (GameConstants.isEvan(chr.getJob()) || GameConstants.isResist(chr.getJob()) || GameConstants.isMercedes(chr.getJob())) {
            final int size = chr.getRemainingSpSize();
            mplew.write(size);
            for (int i = 0; i < chr.getRemainingSps().length; i++) {
                if (chr.getRemainingSp(i) > 0) {
                    mplew.write(i + 1);
                    mplew.write(chr.getRemainingSp(i));
                }
            }
        } else {
            mplew.writeShort(chr.getRemainingSp()); // remaining sp
        }
        //4A B7 00 00 //cid of loveliness
        mplew.writeInt(chr.getExp()); // exp
        mplew.writeInt(chr.getFame()); // fame
        mplew.writeInt(chr.getGachExp()); // Gachapon exp
        mplew.writeInt(chr.getMapId()); // current map id	
        mplew.write(chr.getInitialSpawnpoint()); // spawnpoint
        mplew.writeInt(0); // online time in seconds
        mplew.writeShort(chr.getSubcategory()); //1 here = db, 2 = cannoner
        if (GameConstants.isDemon(chr.getJob())) {
            mplew.writeInt(chr.getDemonMarking());
        }
        mplew.write(chr.getFatigue());
        mplew.writeInt(GameConstants.getCurrentDate());
        for (MapleTraitType t : MapleTraitType.values()) {
            mplew.writeInt(chr.getTrait(t).getTotalExp()); // total trait point
        }
        for (MapleTraitType t : MapleTraitType.values()) {
            mplew.writeShort(0); // today's trait points
        }
        mplew.writeInt(chr.getStat().pvpExp); //pvp exp
        mplew.write(chr.getStat().pvpRank); //pvp rank
        mplew.writeInt(chr.getBattlePoints()); //pvp points
        mplew.write(5); //idk
        mplew.writeInt(0); // TODO JUMP
        mplew.writeInt(0x95FB64D5); // dwHighDateTime // D5 64 FB 95
        mplew.writeInt(0x137); // dwLowDateTime // found the converter from the server files, will make one soon. =)
    }

    public static final void addCharLook(final MaplePacketLittleEndianWriter mplew, final MapleCharacterLook chr, final boolean mega) {
        mplew.write(chr.getGender());
        mplew.write(chr.getSkinColor());
        mplew.writeInt(chr.getFace());
        mplew.writeInt(chr.getJob());
        mplew.write(mega ? 0 : 1);
        mplew.writeInt(chr.getHair());

        final Map<Byte, Integer> myEquip = new LinkedHashMap<>();
        final Map<Byte, Integer> maskedEquip = new LinkedHashMap<>();
        final Map<Byte, Integer> equip = chr.getEquips();
        for (final Entry<Byte, Integer> item : equip.entrySet()) {
            if (item.getKey() < -127) { //not visible
                continue;
            }
            byte pos = (byte) (item.getKey() * -1);

            if (pos < 100 && myEquip.get(pos) == null) {
                myEquip.put(pos, item.getValue());
            } else if (pos > 100 && pos != 111) {
                pos = (byte) (pos - 100);
                if (myEquip.get(pos) != null) {
                    maskedEquip.put(pos, myEquip.get(pos));
                }
                myEquip.put(pos, item.getValue());
            } else if (myEquip.get(pos) != null) {
                maskedEquip.put(pos, item.getValue());
            }
        }
        for (final Entry<Byte, Integer> entry : myEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF); // end of visible itens
        // masked itens
        for (final Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF); // ending markers

        final Integer cWeapon = equip.get((byte) -111);
        mplew.writeInt(cWeapon != null ? cWeapon : 0);
        mplew.write(GameConstants.isMercedes(chr.getJob()) ? 1 : 0);
        mplew.writeZeroBytes(12); // pets
        if (GameConstants.isDemon(chr.getJob())) {
            mplew.writeInt(chr.getDemonMarking());
        }
    }

    public static final void addExpirationTime(final MaplePacketLittleEndianWriter mplew, final long time) {
        mplew.writeLong(getTime(time));
    }

    public static void addItemPosition(final MaplePacketLittleEndianWriter mplew, final Item item, final boolean trade, final boolean bagSlot) {
        if (item == null) {
            mplew.write(0);
            return;
        }
        short pos = item.getPosition();
        if (pos <= -1) {
            pos *= -1;
            if (pos > 100 && pos < 1000) {
                pos -= 100;
            }
        }
        if (bagSlot) {
            mplew.writeInt((pos % 100) - 1);
        } else if (!trade && item.getType() == 1) {
            mplew.writeShort(pos);
        } else {
            mplew.write(pos);
        }
    }

    public static final void addItemInfo(final MaplePacketLittleEndianWriter mplew, final Item item) {
        addItemInfo(mplew, item, null);
    }

    public static final void addItemInfo(final MaplePacketLittleEndianWriter mplew, final Item item, final MapleCharacter chr) {
        mplew.write(item.getPet() != null ? 3 : item.getType());
        mplew.writeInt(item.getItemId());
        boolean hasUniqueId = item.getUniqueId() > 0 && !GameConstants.isMarriageRing(item.getItemId()) && item.getItemId() / 10000 != 166;
        //marriage rings arent cash items so dont have uniqueids, but we assign them anyway for the sake of rings
        mplew.write(hasUniqueId ? 1 : 0);
        if (hasUniqueId) {
            mplew.writeLong(item.getUniqueId());
        }
        if (item.getPet() != null) { // Pet
            addPetItemInfo(mplew, item, item.getPet(), true);
        } else {
            addExpirationTime(mplew, item.getExpiration());
            mplew.writeInt(chr == null ? -1 : chr.getExtendedSlots().indexOf(item.getItemId()));
            if (item.getType() == 1) {
                final Equip equip = (Equip) item;
                mplew.write(equip.getUpgradeSlots());
                mplew.write(equip.getLevel());
                mplew.writeShort(equip.getStr());
                mplew.writeShort(equip.getDex());
                mplew.writeShort(equip.getInt());
                mplew.writeShort(equip.getLuk());
                mplew.writeShort(equip.getHp());
                mplew.writeShort(equip.getMp());
                mplew.writeShort(equip.getWatk());
                mplew.writeShort(equip.getMatk());
                mplew.writeShort(equip.getWdef());
                mplew.writeShort(equip.getMdef());
                mplew.writeShort(equip.getAcc());
                mplew.writeShort(equip.getAvoid());
                mplew.writeShort(equip.getHands());
                mplew.writeShort(equip.getSpeed());
                mplew.writeShort(equip.getJump());
                mplew.writeMapleAsciiString(equip.getOwner());
                mplew.writeShort(equip.getFlag());
                mplew.write(equip.getIncSkill() > 0 ? 1 : 0);
                mplew.write(Math.max(equip.getBaseLevel(), equip.getEquipLevel())); // Item level
                mplew.writeInt(equip.getExpPercentage() * 100000); // Item Exp... 10000000 = 100%
                mplew.writeInt(equip.getDurability());
                mplew.writeInt(equip.getViciousHammer());
                mplew.writeShort(equip.getPVPDamage());
                mplew.write(equip.getState()); // 17 = rare, 18 = epic, 19 = unique, 20 = legendary, potential flags.
                mplew.write(equip.getEnhance());
                mplew.writeShort(equip.getPotential1());
                if (!hasUniqueId) {
                    mplew.writeShort(equip.getPotential2());
                    mplew.writeShort(equip.getPotential3());
                    mplew.writeShort(equip.getPotential4());
                    mplew.writeShort(equip.getPotential5());
                }
                mplew.writeShort(equip.getSocketState());
                mplew.writeShort(equip.getSocket1() % 10000); // > 0 = mounted, 0 = empty, -1 = none.
                mplew.writeShort(equip.getSocket2() % 10000);
                mplew.writeShort(equip.getSocket3() % 10000);
                mplew.writeLong(equip.getInventoryId() <= 0 ? -1 : equip.getInventoryId()); //some tracking ID
                mplew.writeLong(getTime(-2));
                mplew.writeInt(-1); //?
            } else {
                mplew.writeShort(item.getQuantity());
                mplew.writeMapleAsciiString(item.getOwner());
                mplew.writeShort(item.getFlag());
                if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId()) || item.getItemId() / 10000 == 287) {
                    mplew.writeLong(item.getInventoryId() <= 0 ? -1 : item.getInventoryId());
                }
            }
        }
    }

    public static final void serializeMovementList(final MaplePacketLittleEndianWriter lew, final List<LifeMovementFragment> moves) {
        lew.write(moves.size());
        for (LifeMovementFragment move : moves) {
            move.serialize(lew);
        }
    }

    public static final void addAnnounceBox(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        if (chr.getPlayerShop() != null && chr.getPlayerShop().isOwner(chr) && chr.getPlayerShop().getShopType() != 1 && chr.getPlayerShop().isAvailable()) {
            addInteraction(mplew, chr.getPlayerShop());
        } else {
            mplew.write(0);
        }
    }

    public static final void addInteraction(final MaplePacketLittleEndianWriter mplew, IMaplePlayerShop shop) {
        mplew.write(shop.getGameType());
        mplew.writeInt(((AbstractPlayerStore) shop).getObjectId());
        mplew.writeMapleAsciiString(shop.getDescription());
        if (shop.getShopType() != 1) {
            mplew.write(shop.getPassword().length() > 0 ? 1 : 0); //password = false
        }
        mplew.write(shop.getItemId() % 10);
        mplew.write(shop.getSize()); //current size
        mplew.write(shop.getMaxSize()); //full slots... 4 = 4-1=3 = has slots, 1-1=0 = no slots
        if (shop.getShopType() != 1) {
            mplew.write(shop.isOpen() ? 0 : 1);
        }
    }

    public static final void addCharacterInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        mplew.writeInt(-1);
        mplew.writeInt(-3);
        mplew.writeZeroBytes(7); //5 bytes v99 [byte] [byte] [int] [byte]
        addCharStats(mplew, chr);
        mplew.write(chr.getBuddylist().getCapacity());
        if (chr.getBlessOfFairyOrigin() != null) {
            mplew.write(1);
            mplew.writeMapleAsciiString(chr.getBlessOfFairyOrigin());
        } else {
            mplew.write(0);
        }
        if (chr.getBlessOfEmpressOrigin() != null) {
            mplew.write(1);
            mplew.writeMapleAsciiString(chr.getBlessOfEmpressOrigin());
        } else {
            mplew.write(0);
        }
        final MapleQuestStatus ultExplorer = chr.getQuestNoAdd(MapleQuest.getInstance(GameConstants.ULT_EXPLORER));
        if (ultExplorer != null && ultExplorer.getCustomData() != null) {
            mplew.write(1);
            mplew.writeMapleAsciiString(ultExplorer.getCustomData());
        } else {
            mplew.write(0);
        }
        addInventoryInfo(mplew, chr);
        addSkillInfo(mplew, chr); // 0x100
        addCoolDownInfo(mplew, chr); // 0x8000
        addQuestInfo(mplew, chr);
        addRingInfo(mplew, chr);
        addRocksInfo(mplew, chr); // 0x1000
        addMonsterBookInfo(mplew, chr);
        mplew.writeShort(0);
        mplew.writeShort(0); // New year gift card size // 0x40000
		/*
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeMapleAsciiString("Probably Name");
        mplew.write(0);
        mplew.writeLong(0); // Sent time?
        mplew.writeInt(0);
        mplew.writeMapleAsciiString("Message");
        mplew.write(0);
        mplew.write(0);
        mplew.writeLong(0);
        mplew.writeMapleAsciiString("??");
         */
        chr.QuestInfoPacket(mplew); // 0x80000
        if (chr.getJob() >= 3300 && chr.getJob() <= 3312) { // 0x400000
            addJaguarInfo(mplew, chr);
        }
        mplew.writeShort(0); // Foreach: Short + Long (filetime)
        mplew.writeShort(0); // 0x40 Foreach: Short + Int
        mplew.writeShort(0);
        for (int i = 0; i < 17; i++) { // 0x200
            mplew.writeInt(0); // phantom steal skill stuffs :\
        }
        mplew.writeShort(0);
        final boolean unk2 = true;
        mplew.write(unk2 ? 1 : 0);
        if (unk2) { // 0x100
            mplew.writeShort(0); // For each: int + int
        } else {
            mplew.writeShort(0); // For each: int + int
            mplew.writeShort(0); // For Each, one int
        }
        mplew.writeShort(0); // Additional item effect, Foreach: Short + Short
    }

    public static final void addMonsterBookInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        mplew.writeInt(0); // 0x20000
        if (chr.getMonsterBook().getSetScore() > 0) { // 0x10000
            chr.getMonsterBook().writeFinished(mplew);
        } else {
            chr.getMonsterBook().writeUnfinished(mplew);
        }

        mplew.writeInt(chr.getMonsterBook().getSet()); // 0x80000000
    }

    public static final void addPetItemInfo(final MaplePacketLittleEndianWriter mplew, final Item item, final MaplePet pet, final boolean active) {
        if (item == null) {
            mplew.writeLong(PacketHelper.getKoreanTimestamp((long) (System.currentTimeMillis() * 1.5)));
        } else {
            PacketHelper.addExpirationTime(mplew, item.getExpiration() <= System.currentTimeMillis() ? -1 : item.getExpiration());
        }
        mplew.writeInt(-1);
        mplew.writeAsciiString(pet.getName(), 13);
        mplew.write(pet.getLevel());
        mplew.writeShort(pet.getCloseness());
        mplew.write(pet.getFullness());
        if (item == null) {
            mplew.writeLong(PacketHelper.getKoreanTimestamp((long) (System.currentTimeMillis() * 1.5)));
        } else {
            PacketHelper.addExpirationTime(mplew, item.getExpiration() <= System.currentTimeMillis() ? -1 : item.getExpiration());
        }
        mplew.writeShort(0);
        mplew.writeShort(pet.getFlags());
        mplew.writeInt(pet.getPetItemId() == 5000054 && pet.getSecondsLeft() > 0 ? pet.getSecondsLeft() : 0); //in seconds, 3600 = 1 hr.
        mplew.writeShort(0);
        mplew.write(active ? (pet.getSummoned() ? pet.getSummonedValue() : 0) : 0); // 1C 5C 98 C6 01
        for (int i = 0; i < 4; i++) {
            mplew.write(0); //0x40 before, changed to 0?
        }
    }

    public static final void addShopInfo(final MaplePacketLittleEndianWriter mplew, final MapleShop shop, final MapleClient c) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        mplew.write(shop.getRanks().size() > 0 ? 1 : 0);
        if (shop.getRanks().size() > 0) {
            mplew.write(shop.getRanks().size());
            for (Pair<Integer, String> s : shop.getRanks()) {
                mplew.writeInt(s.left);
                mplew.writeMapleAsciiString(s.right);
            }
        }
        mplew.writeShort(shop.getItems().size() + c.getPlayer().getRebuy().size()); // item count
        for (MapleShopItem item : shop.getItems()) {
            addShopItemInfo(mplew, item, shop, ii, null);
        }
        for (Item i : c.getPlayer().getRebuy()) {
            addShopItemInfo(mplew, new MapleShopItem(i.getItemId(), (int) ii.getPrice(i.getItemId()), i.getQuantity()), shop, ii, i);
        }
    }

    public static final void addShopItemInfo(final MaplePacketLittleEndianWriter mplew, final MapleShopItem item, final MapleShop shop, final MapleItemInformationProvider ii, final Item i) {
        mplew.writeInt(item.getItemId());
        mplew.writeInt(item.getPrice());
        mplew.write(0);
        mplew.writeInt(item.getReqItem());
        mplew.writeInt(item.getReqItemQ());
        mplew.writeInt(item.getExpiration()); // in minutes i think
        mplew.writeInt(item.getMinLevel()); // min level
        mplew.writeInt(item.getCategory()); // 1 = equip, 2 = use, 3 = setup, 4 = etc, 5 = recipe, 6 = scroll, 7 = special, 8 = 7th anniversary, 9 = button, 10 = invitation ticket, 11 = materials, 12 = korean word, 0 = no tab 
        mplew.write(0); // boolean
        mplew.writeInt(0); // 1?
        if (!GameConstants.isThrowingStar(item.getItemId()) && !GameConstants.isBullet(item.getItemId())) {
            mplew.writeShort(1); // stacksize
            mplew.writeShort(item.getBuyable());
        } else {
            mplew.writeZeroBytes(6);
            mplew.writeShort(BitTools.doubleToShortBits(ii.getPrice(item.getItemId())));
            mplew.writeShort(item.getBuyable() > 0 ? item.getBuyable() : ii.getSlotMax(item.getItemId())); // priority to official shops, then we take from wz if don't have.
        }
        mplew.write(i == null ? 0 : 1);
        if (i != null) {
            addItemInfo(mplew, i);
        }
        if (shop.getRanks().size() > 0) {
            mplew.write(item.getRank() >= 0 ? 1 : 0);
            if (item.getRank() >= 0) {
                mplew.write(item.getRank());
            }
        }
    }

    public static final void addJaguarInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        mplew.write(chr.getIntNoRecord(GameConstants.JAGUAR));
        mplew.writeZeroBytes(20); //probably mobID of the 5 mobs that can be captured.
    }

    public static <E extends Buffstat> void writeSingleMask(MaplePacketLittleEndianWriter mplew, E statup) {
        for (int i = GameConstants.MAX_BUFFSTAT; i >= 1; i--) {
            mplew.writeInt(i == statup.getPosition() ? statup.getValue() : 0);
        }
    }

    public static <E extends Buffstat> void writeMask(MaplePacketLittleEndianWriter mplew, Collection<E> statups) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (E statup : statups) {
            mask[statup.getPosition() - 1] |= statup.getValue();
        }
        for (int i = mask.length; i >= 1; i--) {
            mplew.writeInt(mask[i - 1]);
        }
    }

    public static <E extends Buffstat> void writeBuffMask(MaplePacketLittleEndianWriter mplew, Collection<Pair<E, Integer>> statups) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (Pair<E, Integer> statup : statups) {
            mask[statup.left.getPosition() - 1] |= statup.left.getValue();
        }
        for (int i = mask.length; i >= 1; i--) {
            mplew.writeInt(mask[i - 1]);
        }
    }

    public static <E extends Buffstat> void writeBuffMask(MaplePacketLittleEndianWriter mplew, Map<E, Integer> statups) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (E statup : statups.keySet()) {
            mask[statup.getPosition() - 1] |= statup.getValue();
        }
        for (int i = mask.length; i >= 1; i--) {
            mplew.writeInt(mask[i - 1]);
        }
    }
}
