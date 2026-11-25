package bg.workshop.dovetail.ui.diagram

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bg.workshop.dovetail.geometry.FingerJoint
import bg.workshop.dovetail.geometry.JointType
import bg.workshop.dovetail.ui.components.mm
import bg.workshop.dovetail.ui.theme.LocalWorkshopColors
import kotlin.math.min

private val DarkWood = Color(0xFF3B2410)

// ─── Прорез, лагер и гнездо (отгоре) ────────────────────────────────────

/** Лагерът следва двете стени на прореза; фрезерът реже гнездо, по-широко с разликата в диаметрите. */
@Composable
fun FingerSlotDiagram(finger: Double?, slot: Double?, cutter: Double?, bearing: Double?, joint: FingerJoint?) =
    Diagram(250.dp) { kit ->
        if (finger == null || slot == null || cutter == null || bearing == null) return@Diagram
        val s = min(size.width * 0.8f / (slot + 1.4 * finger).toFloat(), 12.dp.toPx())
        val cx = size.width / 2
        val half = (slot / 2 * s).toFloat()
        val fw = (finger * 0.7 * s).toFloat()
        val bodyTop = 14.dp.toPx()
        val fingerTop = 34.dp.toPx()
        val fingerBottom = 128.dp.toPx()

        // Шаблон
        drawRect(kit.c.metalLight, Offset(cx - half - fw, bodyTop), Size(2 * (half + fw), fingerTop - bodyTop))
        drawRect(kit.c.metal, Offset(cx - half - fw, fingerTop), Size(fw, fingerBottom - fingerTop))
        drawRect(kit.c.metal, Offset(cx + half, fingerTop), Size(fw, fingerBottom - fingerTop))

        // Лагер до лявата стена, призрак до дясната; фрезерът — концентричен, пунктир
        val rb = (bearing / 2 * s).toFloat()
        val rc = (cutter / 2 * s).toFloat()
        val by = 84.dp.toPx()
        val left = Offset(cx - half + rb, by)
        val right = Offset(cx + half - rb, by)
        drawCircle(kit.muted.copy(alpha = 0.25f), rb, right)
        drawCircle(kit.muted, rb, right, style = Stroke(1.dp.toPx(), pathEffect = dashed))
        drawCircle(kit.panel, rb, left)
        drawCircle(kit.ink, rb, left, style = Stroke(2.dp.toPx()))
        drawCircle(kit.c.axis, rc, left, style = Stroke(2.dp.toPx(), pathEffect = dashed))
        drawLine(kit.muted, left, right, 1.dp.toPx(), pathEffect = dashed)
        dimension(kit, Offset(cx - half, fingerTop + 14.dp.toPx()), Offset(cx + half, fingerTop + 14.dp.toPx()), "прорез ${slot.mm()}")

        // Детайл с изрязаното гнездо
        val woodTop = 150.dp.toPx()
        val woodBottom = 205.dp.toPx()
        drawRect(kit.c.wood, Offset(cx - half - fw, woodTop), Size(2 * (half + fw), woodBottom - woodTop))
        val sl = left.x - rc
        val sr = right.x + rc
        drawRect(kit.panel, Offset(sl, woodTop), Size(sr - sl, 32.dp.toPx()))
        drawRect(kit.c.woodEdge, Offset(cx - half - fw, woodTop), Size(2 * (half + fw), woodBottom - woodTop), style = Stroke(1.5.dp.toPx()))
        val socketText = joint?.let { "гнездо ${it.socketWidth.mm()}" } ?: "гнездо"
        dimension(kit, Offset(sl, woodBottom + 16.dp.toPx()), Offset(sr, woodBottom + 16.dp.toPx()), "")
        label(kit, socketText, cx, woodBottom + 34.dp.toPx(), Anchor.Center, kit.strong.copy(color = kit.c.dimension))

        // Легенда
        label(kit, "лагер Ø${bearing.mm()}", cx + half + fw + 6.dp.toPx(), by - 10.dp.toPx(), style = kit.strong)
        label(kit, "фрезер Ø${cutter.mm()}", cx + half + fw + 6.dp.toPx(), by + 10.dp.toPx(), style = kit.strong.copy(color = kit.c.axis))
    }

// ─── Прав фрезер с горен лагер (отстрани) ───────────────────────────────

/** Лагерът опира в ръба на шаблона; фрезерът под него реже на дълбочина = дебелина + надстояване. */
@Composable
fun FingerBitDiagram(depth: Double?, cutter: Double?, bearing: Double?, slot: Double?) = Diagram(240.dp) { kit ->
    if (depth == null || cutter == null || bearing == null || slot == null) return@Diagram
    val plate = 5.0
    val shank = 12.0
    val top = 16.dp.toPx()
    val bottomPad = 26.dp.toPx()
    val s = min(
        (size.height - top - bottomPad) / (depth + plate + shank).toFloat(),
        size.width * 0.22f / maxOf(cutter, bearing).toFloat(),
    )
    val cx = size.width * 0.4f
    val y0 = top + ((plate + shank) * s).toFloat()
    val plateTop = y0 - (plate * s).toFloat()
    val tip = y0 + (depth * s).toFloat()
    val rb = (bearing / 2 * s).toFloat()
    val rc = (cutter / 2 * s).toFloat()

    // Детайл
    drawRect(kit.c.wood, Offset(0f, y0), Size(size.width, size.height - y0))
    label(kit, "детайл", 10.dp.toPx(), size.height - 12.dp.toPx(), style = kit.strong.copy(color = DarkWood))
    // Шаблон: лагерът опира в лявата стена на прореза
    val slotRight = cx - rb + (slot * s).toFloat()
    drawRect(kit.c.metal, Offset(0f, plateTop), Size(cx - rb, y0 - plateTop))
    drawRect(kit.c.metal, Offset(slotRight, plateTop), Size(size.width - slotRight, y0 - plateTop))
    label(kit, "шаблон", 10.dp.toPx(), plateTop - 10.dp.toPx())

    // Стебло, лагер, режеща част
    val shankW = rb * 0.9f
    drawRect(kit.muted, Offset(cx - shankW / 2, top), Size(shankW, plateTop - top))
    drawRect(kit.panel, Offset(cx - rb, plateTop), Size(2 * rb, y0 - plateTop))
    drawRect(kit.ink, Offset(cx - rb, plateTop), Size(2 * rb, y0 - plateTop), style = Stroke(2.dp.toPx()))
    drawRect(kit.c.metal.copy(alpha = 0.92f), Offset(cx - rc, y0), Size(2 * rc, tip - y0))
    drawRect(kit.ink, Offset(cx - rc, y0), Size(2 * rc, tip - y0), style = Stroke(1.5.dp.toPx()))
    drawLine(kit.c.axis, Offset(cx, top - 6.dp.toPx()), Offset(cx, tip + 10.dp.toPx()), 1.dp.toPx(), pathEffect = dashed)

    val dimX = cx + maxOf(rb, rc) + 22.dp.toPx()
    dimension(kit, Offset(dimX, y0), Offset(dimX, tip), "${depth.mm()} mm")
    label(kit, "лагер", cx - rb - 6.dp.toPx(), plateTop - 10.dp.toPx(), Anchor.End, kit.strong)
}

// ─── Двата детайла в зацепване ──────────────────────────────────────────

/** Детайл Б е изместен с половин стъпка — зъбите му влизат в гнездата на А. */
@Composable
fun FingerMeshDiagram(joint: FingerJoint?) = Diagram(230.dp) { kit ->
    val j = joint ?: return@Diagram
    val pitches = 4
    val s = (size.width * 0.86f / ((pitches + 0.5) * j.pitch)).toFloat()
    val x0 = (size.width - ((pitches + 0.5) * j.pitch * s).toFloat()) / 2
    val w = (pitches * j.pitch * s).toFloat()
    val off = (j.offset * s).toFloat()
    val lf = 42.dp.toPx()
    val bTop = 34.dp.toPx()
    val bBottom = 76.dp.toPx()
    val aTop = bBottom + lf
    val aBottom = aTop + 42.dp.toPx()

    // Б отгоре, зъбите надолу
    drawRect(kit.c.woodAlt, Offset(x0 + off, bTop), Size(w, bBottom - bTop))
    for (k in 0 until pitches) {
        drawRect(kit.c.woodAlt, Offset(x0 + off + (k * j.pitch * s).toFloat(), bBottom), Size((j.pinWidth * s).toFloat(), lf))
    }
    // А отдолу, зъбите нагоре
    drawRect(kit.c.wood, Offset(x0, aTop), Size(w, aBottom - aTop))
    for (k in 0 until pitches) {
        drawRect(kit.c.wood, Offset(x0 + (k * j.pitch * s).toFloat(), bBottom), Size((j.pinWidth * s).toFloat(), lf))
    }
    drawRect(kit.c.woodEdge, Offset(x0 + off, bTop), Size(w, bBottom - bTop), style = Stroke(1.5.dp.toPx()))
    drawRect(kit.c.woodEdge, Offset(x0, aTop), Size(w, aBottom - aTop), style = Stroke(1.5.dp.toPx()))
    label(kit, "Б", x0 + off + w / 2, (bTop + bBottom) / 2, Anchor.Center, kit.strong.copy(color = DarkWood))
    label(kit, "А", x0 + w / 2, (aTop + aBottom) / 2, Anchor.Center, kit.strong.copy(color = DarkWood))

    // Отместване между левите ръбове
    drawLine(kit.muted, Offset(x0, aBottom), Offset(x0, 12.dp.toPx()), 1.dp.toPx(), pathEffect = dashed)
    drawLine(kit.muted, Offset(x0 + off, bTop), Offset(x0 + off, 12.dp.toPx()), 1.dp.toPx(), pathEffect = dashed)
    dimension(kit, Offset(x0, 16.dp.toPx()), Offset(x0 + off, 16.dp.toPx()), "")
    label(kit, "отместване ${j.offset.mm()} mm", x0 + off + 8.dp.toPx(), 16.dp.toPx(), style = kit.strong.copy(color = kit.c.dimension))
    label(kit, "зъб ${j.pinWidth.mm()} · гнездо ${j.socketWidth.mm()} · хлабина ${j.clearance.mm()}", size.width / 2, aBottom + 18.dp.toPx(), Anchor.Center)
}

// ─── Иконка за вида сглобка ─────────────────────────────────────────────

@Composable
fun JointIcon(joint: JointType, size: Dp = 56.dp) {
    val c = LocalWorkshopColors.current
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawRect(c.woodAlt, Offset.Zero, Size(w, h * 0.5f))
        drawRect(c.wood, Offset(0f, h * 0.5f), Size(w, h * 0.5f))
        val n = 3
        val pitch = w / n
        for (k in 0 until n) {
            val cx = pitch * (k + 0.5f)
            when (joint) {
                JointType.DOVETAIL -> {
                    val path = Path().apply {
                        moveTo(cx - pitch * 0.18f, h * 0.5f)
                        lineTo(cx + pitch * 0.18f, h * 0.5f)
                        lineTo(cx + pitch * 0.32f, h * 0.18f)
                        lineTo(cx - pitch * 0.32f, h * 0.18f)
                        close()
                    }
                    drawPath(path, c.wood)
                }
                JointType.FINGER -> drawRect(c.wood, Offset(cx - pitch * 0.25f, h * 0.18f), Size(pitch * 0.5f, h * 0.32f))
            }
        }
        drawRect(c.woodEdge, Offset.Zero, Size(w, h), style = Stroke(1.5.dp.toPx()))
    }
}
