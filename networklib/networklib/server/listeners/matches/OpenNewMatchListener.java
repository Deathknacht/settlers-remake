package networklib.server.listeners.matches;

import java.io.IOException;

import networklib.NetworkConstants;
import networklib.NetworkConstants.ENetworkKey;
import networklib.common.packets.MatchInfoPacket;
import networklib.common.packets.OpenNewMatchPacket;
import networklib.infrastructure.channel.GenericDeserializer;
import networklib.infrastructure.channel.listeners.PacketChannelListener;
import networklib.server.IServerManager;
import networklib.server.game.Match;
import networklib.server.game.Player;

/**
 * This listener is called when a client request to open up a new {@link Match}. After the match has successfully been created, the client will
 * receive a {@link MatchInfoPacket}.
 * 
 * @author Andreas Eberle
 * 
 */
public class OpenNewMatchListener extends PacketChannelListener<OpenNewMatchPacket> {

	private final IServerManager serverManager;
	private final Player player;

	public OpenNewMatchListener(IServerManager serverManager, Player player) {
		super(NetworkConstants.ENetworkKey.REQUEST_OPEN_NEW_MATCH, new GenericDeserializer<OpenNewMatchPacket>(OpenNewMatchPacket.class));
		this.serverManager = serverManager;
		this.player = player;
	}

	@Override
	protected void receivePacket(ENetworkKey key, OpenNewMatchPacket packet) throws IOException {
		serverManager.createNewMatch(packet, player);
	}

}
