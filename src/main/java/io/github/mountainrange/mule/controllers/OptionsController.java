package io.github.mountainrange.mule.controllers;

import io.github.mountainrange.mule.Config;
import io.github.mountainrange.mule.enums.TerrainType;
import io.github.mountainrange.mule.MULE;
import io.github.mountainrange.mule.SceneLoader;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the options dialog
 */
public class OptionsController implements Initializable, SceneAgent {

	private SceneLoader sceneLoader;
	private MULE mule;

	@FXML
	private ComboBox<String> fadeCombo;

	@FXML
	private ComboBox<String> textureCombo;

	@FXML
	private Button backButton;

	@FXML
	private Slider volumeSlider;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		backButton.setCancelButton(true);
		assert fadeCombo != null : "fx:id=\"myChoices\" was not injected: check your FXML file 'foo.fxml'.";
		fadeCombo.setItems(FXCollections.observableArrayList());
		fadeCombo.getItems().add("TRUE");
		fadeCombo.getItems().add("FALSE");
		textureCombo.getItems().addAll("fancy", "mspaint", "plain", "dankmemes");

		volumeSlider.valueProperty().addListener((observableValue, number, t1) -> {
			float volume = (float) (volumeSlider.getValue() / volumeSlider.getMax());
			Config.getInstance().soundManager.setMasterVolume(volume);
			Config.getInstance().soundManager.setPlayingVolume(volume);
		});
	}

	public void setSceneParent(SceneLoader sceneLoader, MULE mule){
		this.sceneLoader = sceneLoader;
		this.mule = mule;
	}

	public void onSetScene() {
	}

	@FXML
	private void handleBackAction(ActionEvent e) {
		sceneLoader.goBack();
	}

	@FXML
	private void handleEnterAction(Event e) {
		fadeCombo.getSelectionModel().select(String.valueOf(Config.getInstance().fadeEnabled).toUpperCase());
		textureCombo.getSelectionModel().select(String.valueOf(Config.getInstance().texturePack));
	}

	@FXML
	private void handleFadeAction(ActionEvent e) {
		Config.getInstance().fadeEnabled = Boolean.parseBoolean(fadeCombo.getSelectionModel().getSelectedItem().toLowerCase());
	}

	@FXML
	private void handleTextureAction(ActionEvent e) {
		Config.getInstance().texturePack = textureCombo.getSelectionModel().getSelectedItem();
		TerrainType.changePack(Config.getInstance().texturePack);
	}

}
