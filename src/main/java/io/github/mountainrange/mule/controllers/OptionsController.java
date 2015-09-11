package io.github.mountainrange.mule.controllers;

import io.github.mountainrange.mule.Config;
import io.github.mountainrange.mule.MULE;
import io.github.mountainrange.mule.SceneLoader;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by Matthew Keezer on 9/9/2015.
 */
public class OptionsController implements Initializable, SceneAgent {

	private SceneLoader sceneLoader;
	private MULE mule;

	@FXML
	private ComboBox<String> fadeCombo;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		assert fadeCombo != null : "fx:id=\"myChoices\" was not injected: check your FXML file 'foo.fxml'.";
		fadeCombo.setItems(FXCollections.observableArrayList());
		fadeCombo.getItems().add("TRUE");
		fadeCombo.getItems().add("FALSE");
	}

	public void setSceneParent(SceneLoader sceneLoader, MULE mule){
		this.sceneLoader = sceneLoader;
		this.mule = mule;
	}

	@FXML
	private void handleBackAction(ActionEvent e) {
		sceneLoader.goBack();
	}

	@FXML
	private void handleEnterAction(Event e) {
		fadeCombo.getSelectionModel().select(String.valueOf(Config.fadeEnabled).toUpperCase());
	}

	@FXML
	private void handleFadeAction(ActionEvent e) {
		Config.fadeEnabled = Boolean.parseBoolean(fadeCombo.getSelectionModel().getSelectedItem().toLowerCase());
	}

}
