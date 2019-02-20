/**
 *	Launches the user interface for the Huffman assignment.
 **/

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HuffMain extends Application {

	public static final double WIDTH = 600;
	public static final double HEIGHT = 600;

	public static void main(String[] args) {
		launch(args);
	}

	public void start(Stage window) {
		window.setWidth(WIDTH);
		window.setHeight(HEIGHT);
		window.setResizable(false);
		window.setTitle("Huffman");

		HuffViewer viewer = new HuffViewer(new HuffProcessor());
		window.setScene(new Scene(viewer.createLayout(WIDTH, HEIGHT), WIDTH, HEIGHT));

		window.show();
	}
}