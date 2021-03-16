package org.dragonskulle.game.building.stat;

interface DoubleMap {
	
	/**
     * Useful for mapping the level to between two doubles.
     *
     * @param valueMin The lowest possible value of the stat.
     * @param valueMax The highest possible value of the stat.
     * @param level The current level.
     * @param levelMin The lowest possible level.
     * @param levelMax The highest possible level.
     * @return The value, between {@code valueMin} and {@code valueMax}, based on the specified
     *     {@code level}.
     */
    default double map(double valueMin, double valueMax, double level, double levelMin, double levelMax) {
        return valueMin + (((level - levelMin) * (valueMax - valueMin)) / (levelMax - levelMin));
    } 
	
}
