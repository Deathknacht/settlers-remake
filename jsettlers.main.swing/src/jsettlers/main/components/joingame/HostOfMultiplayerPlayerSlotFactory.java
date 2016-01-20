/*******************************************************************************
 * Copyright (c) 2015
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.main.components.joingame;

import jsettlers.common.ai.EPlayerType;
import jsettlers.graphics.startscreen.SettingsManager;
import jsettlers.logic.map.MapLoader;

/**
 * @author codingberlin
 */
public class HostOfMultiplayerPlayerSlotFactory implements PlayerSlotFactory {

	@Override public PlayerSlot createPlayerSlot(byte slot, MapLoader mapLoader) {
		PlayerSlot playerSlot = new PlayerSlot();
		if (slot == 0) {
			SettingsManager settingsManager = SettingsManager.getInstance();
			playerSlot.setPlayerName(settingsManager.getPlayer().getName());

		}
		playerSlot.setPossibleTypes(new EPlayerType[] {EPlayerType.HUMAN});
		playerSlot.setSlotAndTeams((byte) mapLoader.getMaxPlayers());
		playerSlot.setSlot(slot);
		playerSlot.setTeam((byte) (slot < mapLoader.getMaxPlayers() / 2 ? 0 : 1));
		playerSlot.disableAllInputs();
		return playerSlot;
	}

}