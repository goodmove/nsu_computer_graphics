package ru.nsu.fit.g15201.boltava.view

import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.{ScrollPane, ToolBar}
import javafx.scene.image.{ImageView, WritableImage}
import javafx.scene.input.MouseEvent
import javafx.scene.layout._
import javafx.scene.paint.Color
import javafx.stage.Window

import ru.nsu.fit.g15201.boltava.model.canvas.{HexagonalGridController, IDrawable, IGridController, ImageDrawable}
import ru.nsu.fit.g15201.boltava.model.canvas.geometry.DoublePoint
import ru.nsu.fit.g15201.boltava.model.graphics.{BresenhamLineCreator, IColorFiller, ScanLineFiller}
import ru.nsu.fit.g15201.boltava.model.logic._

class MainViewController extends ICellStateObserver {

  private val DEFAULT_CONFIG_PATH = "./config.txt"

  private var drawable: IDrawable = _
  private var colorFiller: IColorFiller = _
  private var gridController: IGridController = _
  private var gameController: IGameLogicController = _

  private val ALIVE_CELL_COLOR = Color.GRAY
  private val DEAD_CELL_COLOR = Color.WHITE

  private val GRID_WIDTH = 40
  private val GRID_HEIGHT = 40
  private val CELL_SIDE_SIZE = 15

  @FXML private var root: VBox = _
  @FXML private var toolbar: ToolBar = _
  @FXML private var gameFieldImageView: ImageView = _
  @FXML private var scrollPane: ScrollPane = _

  private var window: Window = _
  private var gridImage: WritableImage = _

  // ************************* Controller initialization *************************

  @FXML
  private def initialize(): Unit = {
    createNewGrid(DEFAULT_CONFIG_PATH)
    setEventHandlers()
    colorFiller = new ScanLineFiller()
    VBox.setVgrow(scrollPane, Priority.ALWAYS)
  }

  private def setEventHandlers(): Unit = {
    gameFieldImageView.setOnMouseClicked((event: MouseEvent) => {
//      println("hello")
      onFieldDragOrClick((event.getX, event.getY))
      event.consume()
    })

    gameFieldImageView.setOnDragDetected((event: MouseEvent) => {
      gameFieldImageView.startFullDrag()
      event.consume()
    })

    gameFieldImageView.setOnMouseDragOver((event: MouseEvent) => {
      onFieldDragOrClick((event.getX, event.getY))
      event.consume()
    })

  }

  private def createNewGrid(configPath: String) = {
    try {
      val gridParameters = ConfigReader.parseConfig(configPath)
      if (gameController != null) {
        gameController.unsubscribe(this)
      }
      gridController = new HexagonalGridController(gridParameters.cellSideSize)
      gameController = new GameController(gridController)
      gameController.setGridParams(gridParameters)
      gameController.subscribe(this)
      drawCellGrid()
      fillAliveCells(gridParameters.aliveCells)
    } catch {
      case e: Exception =>
        println(e.getMessage)
        AlertHelper.showError(window, "Failed to read configuration file", e.getMessage)
    }
  }

  private def fillAliveCells(aliveCells: Array[(Int, Int)]): Unit = {
    Platform.runLater(() => {
      aliveCells
        .map(coords => gameController.getCells(coords._1)(coords._2))
        .foreach(cell => gameController.onCellClicked(cell))
    })
  }

  // ************************* FXML events *************************

  @FXML
  protected def onPlay(event: MouseEvent): Unit = {
    println("onPlay")
    window = toolbar.getScene.getWindow

    if (!gameController.isGameModelSet) {
      AlertHelper.showError(
        window,
        "Game model not chosen",
        "Open a game model or create a new one by pressing corresponding toolbar buttons."
      )
      return
    }

    if (gameController.isGameFinished) {
      AlertHelper.showWarning(
        window,
        "Game has finished",
        "Choose alive cells to watch life happen."
      )
      return
    }

    // continue game in case it's already started
    if (gameController.isGameStarted) {
      gameController.start()
      return
    }

    gameController.start()
  }

  @FXML
  protected def onPause(event: MouseEvent): Unit = {
    // if game started, pause game
    println("onPause")
    gameController.pause()
  }

  @FXML
  protected def onReset(event: MouseEvent): Unit = {
    // stop game and clear field
    println("onClearField")
    onPause(event)
    gameController.reset()
  }

  @FXML
  protected def onNextStep(event: MouseEvent): Unit = {
    // pause game and make next step
    println("onNextStep")
    gameController.nextStep()
  }

  private def drawCell(drawable: IDrawable, cell: Cell): Unit = {
    val vertices = cell.getVertices
    val verticesCount = vertices.length
    for (i <- vertices.indices) {
      val linePoints = BresenhamLineCreator.getLinePoints(vertices(i), vertices((i + 1) % verticesCount))
      drawable.draw(linePoints)
    }
  }

  private def drawCellGrid(): Unit = {
    val (width, height) = gridController.getCartesianFieldSize(GRID_WIDTH, GRID_HEIGHT)
    gridImage = new WritableImage(width.ceil.toInt, height.ceil.toInt)
    gameFieldImageView.setImage(gridImage)
    drawable = new ImageDrawable(gridImage)

    Platform.runLater(() => {
      gameController.getCells.foreach(_.foreach(cell => drawCell(drawable, cell)))
    })
  }

  private def onFieldDragOrClick(point: DoublePoint): Unit = {
    val cellCoords = gridController.getCellByPoint(point)
    val cellGrid = gameController.getCells
    println(s"$point -> $cellCoords")
    if (cellCoords.x < 0 || cellCoords.y < 0 ||
      cellCoords.x >= cellGrid.length || cellCoords.y >= cellGrid(0).length) return

    val cell = gameController.getCells(cellCoords.x)(cellCoords.y)
    gameController.onCellClicked(cell)
  }

  private def fillCell(cell: Cell, color: Color): Unit = {
    colorFiller.fillCell(drawable, cell, color)
  }

  // ************************* ICellStateObserver *************************

  override def onCellStateChange(cell: Cell): Unit = {
    val color = cell.getState match {
      case State.ALIVE => ALIVE_CELL_COLOR
      case State.DEAD => DEAD_CELL_COLOR
    }

    fillCell(cell, color)
  }

  override def onCellsStateChange(cells: Array[Array[Cell]]): Unit = {
    Platform.runLater(() => {
      cells.foreach(_.foreach(onCellStateChange))
    })
  }

}
