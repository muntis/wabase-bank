package uniso.app

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.{CellStyle, Font, Row, Sheet, Workbook}
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.wabase.TableResultRenderer

import java.io.OutputStream


class XlsStreamer(val stream: OutputStream, workbook: Workbook) {

  var sheet: Sheet = null
  var row: Row = null

  val boldStyle: CellStyle = {
    val style = workbook.createCellStyle

    val font: Font = workbook.createFont
    font.setBold(true)
    style.setFont(font)
    style
  }

  def startTable(worksheetName: String): Unit = {
    sheet = workbook.createSheet(worksheetName)
  }
  def startRow: Unit = {
    val rowNum = sheet.getLastRowNum + 1
    row = sheet.createRow(rowNum)
  }

  def cell(value: Any, style: CellStyle = null): Unit = {
    val cel = row.createCell(Option(row.getLastCellNum).filter(_ > 0).map(_ + 0).getOrElse(0))
    if (style != null) cel.setCellStyle(style)
    cel.setCellValue(valueToString(value))
  }

  def valueToString(value: Any) = value match{
    case null => ""
    case rest => rest.toString
  }

  def endWorkbook = {
    workbook.write(stream)
  }

}

class XssfResultRenderer(writer: OutputStream) extends XlsResultRenderer(writer, new SXSSFWorkbook())
class HssfResultRenderer(writer: OutputStream) extends XlsResultRenderer(writer, new HSSFWorkbook())

class XlsResultRenderer(writer: OutputStream, workbook: Workbook, worksheetName: String = "data", maxRowCount: Int = -1) extends TableResultRenderer {
  val streamer = new XlsStreamer(writer, workbook)
  override def renderHeader() = {
    streamer.startTable(worksheetName)
  }
  override def renderRowStart()             = streamer.startRow
  override def renderHeaderCell(value: Any) = streamer.cell(value, streamer.boldStyle)
  override def renderCell(value: Any)       = streamer.cell(value)
  override def renderRowEnd()               = {}
  override def renderFooter() = {
    streamer.endWorkbook
    writer.flush
  }
}