package ru.nsu.fit.g15201.boltava.model.canvas

import ru.nsu.fit.g15201.boltava.model.canvas.geometry.{DoublePoint, Point}
import ru.nsu.fit.g15201.boltava.model.logic.Cell


/**
  * This class is intended to control various grid systems.
  *
  * Creates arbitrary cell space with its own coordinate system
  *
  */
trait IGridController {

  def generateGrid(width: Int, height: Int): Array[Array[Cell]]

  /**
    * Get cartesian coordinates for cell with coordinates `point`,
    * given in terms of this space
    * @param point point to convert to Cartesian coordinates
    * @return new point, which represents the passed point in Cartesian coordinates
    */
  def getCellCenter(point: Point): DoublePoint

  /**
    * Get cell coordinate by Cartesian coordinate point
    *
    * @param point a point in Cartesian coordinate system
    * @return cell coordinate in custom (internal) coordinate system
    */
  def getCellByPoint(point: DoublePoint): Point

  def getCellNeighbors(point: Point): Array[Point]

  def getCellDistantNeighbors(point: Point): Array[Point]

  def getCartesianFieldSize(width: Int, height: Int): (Double, Double)

}
