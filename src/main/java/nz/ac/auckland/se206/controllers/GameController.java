package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class GameController {

  // --- Static flags ---
  private static boolean isFirstTimeInit = true;

  // --- FXML injected controls ---
  @FXML private Pane contentPane;
  @FXML private Label timer;
  @FXML private Button btnJudge;
  @FXML private Button btnReturn;

  // Cached scenes
  private Parent courtRoomRoot;
  private Parent patrolRoot;
  private PatrolController patrolController; // retain controller to trigger one-time intro
  private Parent deliveryRoot;
  private Parent securityRoot;
  private Parent verdictRoot;

  private Timeline phaseOneTimer;
  private Timeline phaseTwoTimer;
  private int phaseOneSecondsLeft;
  private int phaseTwoSecondsLeft;

  /**
   * Initializes the room view. If it's the first time initialization, it will provide instructions
   * via text-to-speech.
   */
  public void initialize() {
    if (isFirstTimeInit) {
      System.out.println("Initialize called!");
      // Load and cache the courtroom scene
      courtRoomRoot = loadScene("/fxml/courtRoom.fxml");
      patrolRoot = loadScene("/fxml/patrol.fxml");
      deliveryRoot = loadScene("/fxml/delivery.fxml");
      securityRoot = loadScene("/fxml/security.fxml");
      verdictRoot = loadScene("/fxml/verdict.fxml");

      // Set initial courtroom scene
      setContent(courtRoomRoot);
      startPhaseOneTimer();
      isFirstTimeInit = false;
    }
  }

  /** Loads an FXML file and returns its root node. */
  private Parent loadScene(String fxmlPath) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
      Parent root = loader.load();
      // If loading courtroom, inject this GameController
      if (fxmlPath.equals("/fxml/courtRoom.fxml")) {
        CourtRoomController controller = loader.getController();
        controller.setGameController(this);
      } else if (fxmlPath.equals("/fxml/patrol.fxml")) {
        // Keep reference for triggering intro audio after scene switch
        patrolController = loader.getController();
      }
      return root;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /** Sets the given root node as the content of the contentPane. */
  public void setContent(Parent root) {
    if (root == null) {
      System.err.println("setContent called with null root (scene failed to load)");
      return;
    }
    contentPane.getChildren().setAll(root);
  }

  public void showCourtRoom() {
    setContent(courtRoomRoot);
  }

  public void showPatrol() {
    setContent(patrolRoot);
    // Trigger the patrol intro audio exactly once (lazy – only once user actually enters scene)
    if (patrolController != null) {
      patrolController.playIntroIfNeeded();
    }
  }

  public void showDelivery() {
    setContent(deliveryRoot);
  }

  public void showSecurity() {
    setContent(securityRoot);
  }

  public void showVerdict() {
    setContent(verdictRoot);
  }

  @FXML
  private void onBtnReturnCourtRoomAction() {
    setContent(courtRoomRoot);
  }

  private void startPhaseOneTimer() {
    phaseOneSecondsLeft = 120; // 2 minutes
    updateTimerLabel(phaseOneSecondsLeft);
    phaseOneTimer =
        new Timeline(
            new KeyFrame(
                Duration.seconds(1),
                event -> {
                  phaseOneSecondsLeft--;
                  updateTimerLabel(phaseOneSecondsLeft);
                  if (phaseOneSecondsLeft <= 0) {
                    phaseOneTimer.stop();
                    btnJudge.setDisable(true);
                    btnReturn.setDisable(true);
                    showVerdict();
                    startPhaseTwoTimer();
                  }
                }));
    phaseOneTimer.setCycleCount(phaseOneSecondsLeft);
    phaseOneTimer.play();
  }

  private void startPhaseTwoTimer() {
    phaseTwoSecondsLeft = 10;
    updateTimerLabel(phaseTwoSecondsLeft);
    phaseTwoTimer =
        new Timeline(
            new KeyFrame(
                Duration.seconds(1),
                event -> {
                  phaseTwoSecondsLeft--;
                  updateTimerLabel(phaseTwoSecondsLeft);
                  if (phaseTwoSecondsLeft <= 0) {
                    phaseTwoTimer.stop();
                    onDecisionPhaseEnd();
                  }
                }));
    phaseTwoTimer.setCycleCount(phaseTwoSecondsLeft);
    phaseTwoTimer.play();
  }

  private void updateTimerLabel(int secondsLeft) {
    int minutes = secondsLeft / 60;
    int seconds = secondsLeft % 60;
    timer.setText(String.format("%02d:%02d", minutes, seconds));
  }

  @FXML
  private void onBtnJudgeAction() {
    if (phaseOneTimer != null && phaseOneTimer.getStatus() == Timeline.Status.RUNNING) {
      btnJudge.setDisable(true);
      btnReturn.setDisable(true);
      showVerdict();
      phaseOneTimer.stop();
      startPhaseTwoTimer();
    }
  }

  private void onDecisionPhaseEnd() {}
}
