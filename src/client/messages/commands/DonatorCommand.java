package client.messages.commands;

import constants.ServerConstants.PlayerGMRank;

/**
 *
 * @author Emilyx3
 */
public class DonatorCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.DONATOR;
    }
}
