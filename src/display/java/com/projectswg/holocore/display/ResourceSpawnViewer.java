/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.display;

import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.services.crafting.resource.galactic.GalacticResourceLoader;
import com.projectswg.holocore.services.crafting.resource.galactic.GalacticResourceSpawn;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ResourceSpawnViewer extends Application {
	
	private static final int	MAP_SIZE					= 16384;
	private static final double	POSITION_GAUSSIAN_FACTOR	= MAP_SIZE / 2 * Math.sqrt(2);
	private static final int	STAGE_SIZE					= 800;
	
	private static final Terrain [] ALL_PLANETS = new Terrain[] {
		Terrain.CORELLIA,	Terrain.DANTOOINE,		Terrain.DATHOMIR,
		Terrain.ENDOR,		Terrain.KASHYYYK_MAIN,	Terrain.LOK,
		Terrain.MUSTAFAR,	Terrain.NABOO,			Terrain.RORI,
		Terrain.TALUS,		Terrain.TATOOINE,	Terrain.YAVIN4
	};
	private static final Color [] COLORS = new Color[] {
			Color.BLACK, Color.LIGHTGRAY, Color.BLUE, Color.MAGENTA,
			Color.CYAN, Color.ORANGE, Color.DARKGRAY, Color.PINK, Color.GRAY,
			Color.RED, Color.GREEN, Color.YELLOW
	};
	
	private ComboBox<Terrain> terrainCombo;
	private Button saveButton;
	private Canvas resourceCanvas;
	
	public static void main(String [] args) {
		Application.launch(args);
	}
	
	@Override
	public void start(Stage primaryStage) {
		VBox root = new VBox();
		HBox topPanel = new HBox();
		root.getChildren().add(topPanel);
		root.getChildren().add(resourceCanvas = new Canvas(800, 800));
		topPanel.getChildren().add(terrainCombo = new ComboBox<>());
		topPanel.getChildren().add(saveButton = new Button("Save"));
		setupTopPanel();
		primaryStage.setScene(new Scene(root, STAGE_SIZE, STAGE_SIZE + 20));
		primaryStage.show();
	}
	
	private void setupTopPanel() {
		terrainCombo.setItems(FXCollections.observableArrayList(ALL_PLANETS));
		terrainCombo.valueProperty().addListener((val, o, n) -> onTerrainChanged(n));
		terrainCombo.setValue(Terrain.TATOOINE);
		saveButton.setOnAction(e -> save());
	}
	
	private void save() {
		WritableImage writableImage = new WritableImage(STAGE_SIZE, STAGE_SIZE);
        resourceCanvas.snapshot(null, writableImage);
        try {
			ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", new File("resource_spawns.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void onTerrainChanged(Terrain terrain) {
		GraphicsContext gc = resourceCanvas.getGraphicsContext2D();
		gc.clearRect(0, 0, STAGE_SIZE, STAGE_SIZE);
		for (int x = 0; x < STAGE_SIZE/2; x++) {
			for (int y = 0; y < STAGE_SIZE/2; y++) {
				double dist = Math.sqrt(Math.pow(STAGE_SIZE/2-x, 2)+Math.pow(STAGE_SIZE/2-y, 2));
				double opacity = getProbabilityOpacity(dist/STAGE_SIZE*MAP_SIZE);
				gc.setStroke(new Color(0, 0, 0, opacity * 0.75));
				gc.strokeLine(x, y, x, y);
				gc.strokeLine(STAGE_SIZE-x, STAGE_SIZE-y, STAGE_SIZE-x, STAGE_SIZE-y);
				gc.strokeLine(STAGE_SIZE-x, y, STAGE_SIZE-x, y);
				gc.strokeLine(x, STAGE_SIZE-y, x, STAGE_SIZE-y);
			}
		}
		GalacticResourceLoader loader = new GalacticResourceLoader();
		List<GalacticResourceSpawn> spawns = loader.loadSpawns();
		int index = 0;
		for (GalacticResourceSpawn spawn : spawns) {
			if (spawn.getTerrain() != terrain)
				continue;
			Color c = COLORS[(index++) % COLORS.length];
			gc.setFill(c.deriveColor(0, 1, 1, 0.7));
			double x = convertToStage(spawn.getX());
			double z = convertToStage(spawn.getZ());
			double width = (double) spawn.getRadius() / (MAP_SIZE/2) * STAGE_SIZE;
			gc.fillOval(x-width/2, STAGE_SIZE-z-width/2, width, width);
			gc.setFill(Color.BLACK);
			gc.fillText(Long.toString(spawn.getResourceId()), convertToStage(spawn.getX()) - getHorizontalShift(spawn.getResourceId()), STAGE_SIZE-convertToStage(spawn.getZ())+5);
		}
	}
	
	private int convertToStage(double x) {
		return (int) (x / (MAP_SIZE/2) * STAGE_SIZE/2) + STAGE_SIZE/2;
	}
	
	
	private int getHorizontalShift(long resourceId) {
		if (resourceId < 10)
			return 4;
		if (resourceId < 100)
			return 8;
		return 12;
	}
	
	private double getProbabilityOpacity(double dist) {
		return getGaussianY(((dist / POSITION_GAUSSIAN_FACTOR) - 0.5) * 6);
	}
	
	private double getGaussianY(double x) {
		return Math.pow(Math.exp(-((x * x) / 2)), 1 / (Math.sqrt(2 * Math.PI))); 
	}
	
}
