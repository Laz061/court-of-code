package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.shape.Rectangle;

/**
 * Controller class for the room view. Handles user interactions within the room where the user can
 * chat with customers and guess their profession.
 */
public class CourtRoomController {
  private static boolean isFirstTimeInit = true;

  @FXML private Rectangle rectPatrol;
  @FXML private Rectangle rectDelivery;
  @FXML private Rectangle rectSecurity;
  @FXML private Label lblProfession;
  @FXML private Button btnGuess;

  private GameController gameController;

  /**
   * Initializes the room view. If it's the first time initialization, it will provide instructions
   * via text-to-speech.
   */
  @FXML
  public void initialize() {
    if (isFirstTimeInit) {

      isFirstTimeInit = false;
    }
  }

  public void setGameController(GameController gameController) {
    this.gameController = gameController;
  }

  @FXML
  private void handlePatrolClick() throws IOException {
    gameController.showPatrol();
  }

  @FXML
  private void handleDeliveryClick() throws IOException {
    gameController.showDelivery();
  }

  @FXML
  private void handleSecurityClick() throws IOException {
    gameController.showSecurity();
  }
}
