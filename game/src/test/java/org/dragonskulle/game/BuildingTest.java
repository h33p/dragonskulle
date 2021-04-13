package org.dragonskulle.game;

import org.dragonskulle.game.building.Building;
import org.junit.Test;

public class BuildingTest {

	@Test
    public void buildingTest() {
		Building buildingOne = new Building();
		buildingOne.onAwake();
		Building buildingTwo = new Building();
		buildingTwo.onAwake();
		int won = 0;
		int attempts = 10000;
		
		for (int i = 0; i <attempts; i++) {
			if (buildingOne.attack(buildingTwo)) {
				won++;
			}
		}
		
		System.out.println(won);
		System.out.println(attempts);
		System.out.println((float)won/(float)attempts);
	}
}
