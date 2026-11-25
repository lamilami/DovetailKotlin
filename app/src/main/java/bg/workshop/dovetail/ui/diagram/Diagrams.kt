package bg.workshop.dovetail.ui.diagram

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bg.workshop.dovetail.geometry.BitProfile
import bg.workshop.dovetail.geometry.JigSpec
import bg.workshop.dovetail.ui.components.mm
import bg.workshop.dovetail.ui.theme.LocalWorkshopColors
import bg.workshop.dovetail.ui.theme.WorkshopColors
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// ─── Обща инфраструктура ────────────────────────────────────────────────

/** Всичко, от което една схема има нужда, събрано на едно място. */
class Kit(
    val c: WorkshopColors,
    val tm: TextMeasurer,
    val text: TextStyle,
    val strong: TextStyle,
    val ink: Color,
    val muted: Color,
    val panel: Color,
)

@Composable
internal fun Diagram(height: Dp, draw: DrawScope.(Kit) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val base = MaterialTheme.typography.labelMedium
    val kit = Kit(
        c = LocalWorkshopColors.current,
        tm = rememberTextMeasurer(),
        text = base.copy(color = scheme.onSurfaceVariant),
        strong = base.copy(color = scheme.onSurface, fontWeight = FontWeight.Bold),
        ink = scheme.onSurface,
        muted = scheme.onSurfaceVariant,
        panel = scheme.surfaceVariant,
    )
    Canvas(Modifier.fillMaxWidth().height(height)) { draw(kit) }
}

internal enum class Anchor { Start, Center, End }

internal fun DrawScope.label(kit: Kit, s: String, x: Float, y: Float, anchor: Anchor = Anchor.Start, style: TextStyle = kit.text) {
    val m = kit.tm.measure(s, style)
    val left = when (anchor) {
        Anchor.Start -> x
        Anchor.Center -> x - m.size.width / 2f
        Anchor.End -> x - m.size.width
    }
    val clamped = left.coerceIn(0f, (size.width - m.size.width).coerceAtLeast(0f))
    drawText(m, topLeft = Offset(clamped, y - m.size.height / 2f))
}

internal fun DrawScope.arrowHead(from: Offset, tip: Offset, color: Color, len: Float = 9.dp.toPx()) {
    val a = atan2(tip.y - from.y, tip.x - from.x)
    val spread = 0.45f
    val p = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(tip.x - len * cos(a - spread), tip.y - len * sin(a - spread))
        lineTo(tip.x - len * cos(a + spread), tip.y - len * sin(a + spread))
        close()
    }
    drawPath(p, color)
}

/** Размерна линия с ограничители в двата края и надпис в средата. */
internal fun DrawScope.dimension(kit: Kit, a: Offset, b: Offset, text: String, color: Color = kit.c.dimension) {
    val w = 1.5.dp.toPx()
    val tick = 6.dp.toPx()
    drawLine(color, a, b, w)
    val horizontal = kotlin.math.abs(b.y - a.y) < kotlin.math.abs(b.x - a.x)
    listOf(a, b).forEach { p ->
        if (horizontal) drawLine(color, Offset(p.x, p.y - tick), Offset(p.x, p.y + tick), w)
        else drawLine(color, Offset(p.x - tick, p.y), Offset(p.x + tick, p.y), w)
    }
    val mid = Offset((a.x + b.x) / 2, (a.y + b.y) / 2)
    val style = kit.strong.copy(color = color)
    if (horizontal) label(kit, text, mid.x, mid.y - 12.dp.toPx(), Anchor.Center, style)
    else label(kit, text, mid.x + 10.dp.toPx(), mid.y, Anchor.Start, style)
}

internal val dashed get() = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))

internal fun DrawScope.fingers(kit: Kit, x0: Float, top: Float, bottom: Float, finger: Float, pitch: Float, from: Int, to: Int) {
    for (k in from..to) {
        val x = x0 + k * pitch
        drawRoundRect(
            kit.c.metal, Offset(x, top), Size(finger, bottom - top),
            CornerRadius(3.dp.toPx()),
        )
    }
}

// ─── 1. Стопери ─────────────────────────────────────────────────────────

/** Гледано отпред на левия край: молив по средата на първия пръст, стоперите спрямо нея. */
@Composable
fun EndStopsDiagram(finger: Double?, slot: Double?) = Diagram(250.dp) { kit ->
    val f = finger ?: 12.7
    val s = slot ?: 12.7
    val pad = 16.dp.toPx()
    val scale = min(4.5.dp.toPx(), (size.width - 2 * pad) / ((f + s) * 4.5).toFloat())
    val fw = (f * scale).toFloat()
    val pitch = ((f + s) * scale).toFloat()
    val x0 = pad + 70.dp.toPx()
    val bodyTop = 24.dp.toPx()
    val bodyBottom = 48.dp.toPx()
    val tipY = 120.dp.toPx()

    // Тяло на шаблона и пръстите
    drawRect(kit.c.metalLight, Offset(pad, bodyTop), Size(size.width - 2 * pad, bodyBottom - bodyTop))
    label(kit, "шаблон", pad + 6.dp.toPx(), (bodyTop + bodyBottom) / 2)
    val count = ceil((size.width - x0) / pitch).toInt()
    fingers(kit, x0, bodyBottom, tipY, fw, pitch, 0, count)

    // Линия с молив
    val line = x0 + fw / 2
    drawLine(kit.c.axis, Offset(line, bodyTop - 10.dp.toPx()), Offset(line, size.height - 8.dp.toPx()), 2.dp.toPx(), pathEffect = dashed)
    label(kit, "молив: ½ от 1-вия пръст", line + 8.dp.toPx(), bodyTop - 12.dp.toPx(), style = kit.strong.copy(color = kit.c.axis))

    // Хоризонтален стопер — вътрешният му ръб е на линията
    val hY = 150.dp.toPx()
    val stopW = 64.dp.toPx()
    val stopH = 16.dp.toPx()
    drawRect(kit.ink, Offset(line - stopW, hY), Size(stopW, stopH))
    label(kit, "хоризонтален стопер", line + 10.dp.toPx(), hY + stopH / 2, style = kit.strong)

    // Вертикален стопер — ½" вдясно
    val vX = line + (JigSpec.STOP_OFFSET * scale).toFloat()
    val vY = 195.dp.toPx()
    drawRect(kit.ink, Offset(vX - stopW, vY), Size(stopW, stopH))
    drawLine(kit.muted, Offset(vX, vY - 14.dp.toPx()), Offset(vX, vY + stopH), 1.dp.toPx(), pathEffect = dashed)
    label(kit, "вертикален стопер", vX + 10.dp.toPx(), vY + stopH / 2, style = kit.strong)
    dimension(kit, Offset(line, vY - 14.dp.toPx()), Offset(vX, vY - 14.dp.toPx()), "")
    label(kit, "${JigSpec.STOP_OFFSET.mm()} mm (½\")", vX + 10.dp.toPx(), vY - 14.dp.toPx(), style = kit.strong.copy(color = kit.c.dimension))
}

// ─── 2. Центриране ──────────────────────────────────────────────────────

@Composable
fun CenteringDiagram(width: Double?, finger: Double?, slot: Double?) = Diagram(190.dp) { kit ->
    if (width == null || finger == null || slot == null) return@Diagram
    val cx = size.width / 2f
    val scale = min(size.width * 0.85f / width.toFloat(), 5.dp.toPx())
    val pitch = ((finger + slot) * scale).toFloat()
    if (pitch < 1f) return@Diagram
    val fw = (finger * scale).toFloat()
    drawRect(kit.c.metalLight, Offset(0f, 12.dp.toPx()), Size(size.width, 28.dp.toPx()))
    val n = min(500, ceil(size.width / pitch).toInt())
    fingers(kit, cx - fw / 2, 30.dp.toPx(), 90.dp.toPx(), fw, pitch, -n, n)

    val bw = (width * scale).toFloat()
    val boardTop = 108.dp.toPx()
    drawRect(kit.c.wood, Offset(cx - bw / 2, boardTop), Size(bw, 50.dp.toPx()))
    drawRect(kit.c.woodEdge, Offset(cx - bw / 2, boardTop), Size(bw, 50.dp.toPx()), style = Stroke(1.5.dp.toPx()))
    drawLine(kit.c.axis, Offset(cx, 0f), Offset(cx, 166.dp.toPx()), 2.dp.toPx())
    dimension(kit, Offset(cx - bw / 2, 178.dp.toPx()), Offset(cx, 178.dp.toPx()), "${(width / 2).mm()} mm")
}

// ─── 3. Номериране на детайлите ─────────────────────────────────────────

/** Чекмедже отгоре: ъгли 1+2 (дясно) се режат вляво на шаблона, 3+4 — вдясно. */
@Composable
fun DrawerPartsDiagram() = Diagram(280.dp) { kit ->
    val w = min(size.width * 0.62f, 240.dp.toPx())
    val h = 120.dp.toPx()
    val left = (size.width - w) / 2
    val top = 30.dp.toPx()
    val t = 12.dp.toPx()
    val right = left + w
    val bottom = top + h

    // Гръб, лице и страни
    drawRect(kit.c.wood, Offset(left, top), Size(w, t))
    drawRect(kit.c.wood, Offset(left, bottom - t), Size(w, t))
    drawRect(kit.c.woodAlt, Offset(left, top + t), Size(t, h - 2 * t))
    drawRect(kit.c.woodAlt, Offset(right - t, top + t), Size(t, h - 2 * t))
    drawRect(kit.c.woodEdge, Offset(left, top), Size(w, h), style = Stroke(1.5.dp.toPx()))
    label(kit, "гръб", size.width / 2, top - 12.dp.toPx(), Anchor.Center)
    label(kit, "лице", size.width / 2, bottom + 12.dp.toPx(), Anchor.Center)
    label(kit, "лява", left - 8.dp.toPx(), top + h / 2, Anchor.End)
    label(kit, "дясна", right + 8.dp.toPx(), top + h / 2)

    val r = 13.dp.toPx()
    val rightColor = kit.c.axis
    val leftColor = kit.c.dimension
    fun badge(n: Int, x: Float, y: Float, color: Color) {
        drawCircle(color, r, Offset(x, y))
        label(kit, "$n", x, y, Anchor.Center, kit.strong.copy(color = Color.White))
    }
    badge(1, right - t / 2, bottom - t / 2, rightColor)
    badge(2, right - t / 2, top + t / 2, rightColor)
    badge(3, left + t / 2, top + t / 2, leftColor)
    badge(4, left + t / 2, bottom - t / 2, leftColor)

    // Шаблонът — коя страна реже кои ъгли
    val jigTop = bottom + 50.dp.toPx()
    val jigH = 44.dp.toPx()
    val jigL = 16.dp.toPx()
    val jigW = size.width - 2 * jigL
    drawRoundRect(kit.panel, Offset(jigL, jigTop), Size(jigW, jigH), CornerRadius(8.dp.toPx()))
    drawLine(kit.muted, Offset(size.width / 2, jigTop + 6.dp.toPx()), Offset(size.width / 2, jigTop + jigH - 6.dp.toPx()), 1.dp.toPx(), pathEffect = dashed)
    label(kit, "ляво на шаблона: 1 + 2", jigL + jigW / 4, jigTop + jigH / 2, Anchor.Center, kit.strong.copy(color = rightColor))
    label(kit, "дясно: 3 + 4", jigL + 3 * jigW / 4, jigTop + jigH / 2, Anchor.Center, kit.strong.copy(color = leftColor))
}

// ─── 4. Закрепване (страничен разрез) ───────────────────────────────────

/**
 * Потребителят е вляво. Вертикалният детайл е отпред, хоризонталният — отгоре, допрени под шаблона.
 * При прав зъб хоризонталният е само опора за шаблона, а шаблонът не се изтегля назад.
 */
@Composable
fun ClampDiagram(thickness: Double?, finger: Boolean = false) = Diagram(270.dp) { kit ->
    val t = (thickness ?: 18.0).toFloat()
    val plate = 4f
    // Обхват в mm: x от -t-26 до 90, y от -t-plate-14 до 75
    val minX = -t - 40f
    val maxX = 90f
    val minY = -t - plate - 16f
    val maxY = 75f
    val s = min(size.width / (maxX - minX), size.height / (maxY - minY))
    val ox = -minX * s + (size.width - (maxX - minX) * s) / 2
    val oy = -minY * s
    fun p(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
    fun rect(color: Color, x1: Float, y1: Float, x2: Float, y2: Float, stroke: Color? = null) {
        drawRect(color, p(x1, y1), Size((x2 - x1) * s, (y2 - y1) * s))
        if (stroke != null) drawRect(stroke, p(x1, y1), Size((x2 - x1) * s, (y2 - y1) * s), style = Stroke(1.5.dp.toPx()))
    }

    // Тяло на шаблона (основа)
    rect(kit.panel, 0f, 0f, 85f, 75f)
    label(kit, "тяло", p(60f, 50f).x, p(60f, 50f).y, Anchor.Center)
    // Хоризонтален детайл: лежи отгоре, допрян до вертикалния
    rect(kit.c.wood, 0f, -t, 78f, 0f, kit.c.woodEdge)
    label(kit, if (finger) "опора" else "хоризонтален", p(40f, -t / 2).x, p(40f, -t / 2).y, Anchor.Center, kit.strong.copy(color = Color(0xFF3B2410)))
    // Вертикален детайл: отпред, горният му край е на нивото на хоризонталния
    rect(kit.c.woodAlt, -t, -t, 0f, 75f, kit.c.woodEdge)
    // Шаблон: при лястовича опашка върхът на пръста е 3 mm назад от лицето на вертикалния детайл
    val tipX = if (finger) -t else -t + JigSpec.TEMPLATE_SETBACK.toFloat()
    rect(kit.c.metal, tipX, -t - plate, 45f, -t)
    label(kit, "шаблон", p(22f, -t - plate - 7f).x, p(22f, -t - plate - 7f).y, Anchor.Center)
    // Притискащи летви
    rect(kit.ink, 55f, -t - 7f, 72f, -t)
    label(kit, "летва", p(63.5f, -t - 13f).x, p(63.5f, -t - 13f).y, Anchor.Center)
    rect(kit.ink, -t - 7f, 30f, -t, 48f)
    label(kit, "предна", p(-t - 9f, 39f).x, p(-t - 9f, 39f).y, Anchor.End)

    // Размери
    if (!finger) {
        dimension(kit, p(-t, -t - plate - 11f), p(tipX, -t - plate - 11f), "")
        label(kit, "${JigSpec.TEMPLATE_SETBACK.mm()} mm", p(tipX, -t - plate - 11f).x + 8.dp.toPx(), p(0f, -t - plate - 11f).y, style = kit.strong.copy(color = kit.c.dimension))
    }
    dimension(kit, p(-t - 4f, -t), p(-t - 4f, 0f), "")
    label(kit, "${t.toDouble().mm()} mm", p(-t - 4f, -t / 2).x - 10.dp.toPx(), p(0f, -t / 2).y, Anchor.End, kit.strong.copy(color = kit.c.dimension))
    label(kit, "вертикален", p(-t / 2, 62f).x, p(-t / 2, 62f).y, Anchor.Center, kit.strong.copy(color = Color(0xFF3B2410)))
}

// ─── 5. Заден ограничител ───────────────────────────────────────────────

/** Страничен изглед: разстояние от върховете на пръстите до ограничителя. */
@Composable
fun BackStopDistanceDiagram(distance: Double?) = Diagram(130.dp) { kit ->
    val pad = 20.dp.toPx()
    val plateY = 62.dp.toPx()
    val plateH = 12.dp.toPx()
    val tip = pad + 10.dp.toPx()
    val stopX = size.width - pad - 34.dp.toPx()
    drawRect(kit.c.metal, Offset(tip, plateY), Size(size.width - pad - tip, plateH))
    label(kit, "пръсти", tip, plateY + plateH + 14.dp.toPx())
    drawRect(kit.ink, Offset(stopX, plateY - 30.dp.toPx()), Size(18.dp.toPx(), 30.dp.toPx()))
    label(kit, "ограничител", stopX + 18.dp.toPx(), plateY + plateH + 14.dp.toPx(), Anchor.End)
    val text = distance?.let { "${it.mm()} mm" } ?: "L"
    dimension(kit, Offset(tip, plateY - 18.dp.toPx()), Offset(stopX, plateY - 18.dp.toPx()), text)
}

/** Отгоре: опорната страна на основата (кръгла или плоска) и центърът на фрезера. */
@Composable
fun RouterBaseDiagram(base: Double?, chord: Double?, flat: Boolean, bit: Double?) = Diagram(200.dp) { kit ->
    if (base == null) return@Diagram
    val r = base / 2
    val ref = if (flat) chord ?: return@Diagram else r
    if (ref > r) return@Diagram
    val pad = 16.dp.toPx()
    val stopW = 10.dp.toPx()
    val scale = min((size.width - 2 * pad - stopW) / (ref + r).toFloat(), (size.height - 2 * pad) / base.toFloat())
    val x0 = pad + stopW
    val cx = x0 + (ref * scale).toFloat()
    val cy = size.height / 2
    val rp = (r * scale).toFloat()

    drawRect(kit.ink, Offset(pad, pad / 2), Size(stopW, size.height - pad))
    clipRect(left = x0) {
        drawCircle(kit.panel, rp, Offset(cx, cy))
        drawCircle(kit.muted, rp, Offset(cx, cy), style = Stroke(2.dp.toPx()))
    }
    if (flat) {
        val half = (sqrt(r * r - ref * ref) * scale).toFloat()
        drawLine(kit.c.axis, Offset(x0, cy - half), Offset(x0, cy + half), 3.dp.toPx())
    }
    bit?.let { drawCircle(kit.c.metal, (it / 2 * scale).toFloat().coerceAtLeast(3.dp.toPx()), Offset(cx, cy)) }
    val cross = 8.dp.toPx()
    drawLine(kit.ink, Offset(cx - cross, cy), Offset(cx + cross, cy), 1.dp.toPx())
    drawLine(kit.ink, Offset(cx, cy - cross), Offset(cx, cy + cross), 1.dp.toPx())
    dimension(kit, Offset(x0, cy + rp * 0.55f), Offset(cx, cy + rp * 0.55f), "${ref.mm()} mm")
    label(kit, if (flat) "плоска страна" else "кръгла основа", x0 + 6.dp.toPx(), cy - rp * 0.55f)
}

// ─── 6. Фрезер под шаблона ──────────────────────────────────────────────

/** Колко излиза фрезерът под пръстите на шаблона. Реален профил, ако ъгълът и височината са въведени. */
@Composable
fun BitDepthDiagram(depth: Double?, bitDiameter: Double?, profile: BitProfile?) = Diagram(240.dp) { kit ->
    val z = depth ?: return@Diagram
    val d = bitDiameter ?: return@Diagram
    val narrow = profile?.narrowDiameter ?: (d * 0.6)
    val h = profile?.cuttingHeight ?: (z * 1.2)
    val plate = 4.0
    val shank = 14.0
    val top = 18.dp.toPx()
    val bottomPad = 22.dp.toPx()
    val s = min(
        (size.height - top - bottomPad) / (z + plate + shank).toFloat(),
        size.width * 0.32f / d.toFloat(),
    )
    val cx = size.width * 0.42f
    val y0 = top + ((plate + shank) * s).toFloat() // долна повърхност на шаблона
    val tipY = y0 + (z * s).toFloat()
    val coneTopY = tipY - (h * s).toFloat()

    // Детайл
    drawRect(kit.c.wood, Offset(0f, y0), Size(size.width, size.height - y0))
    label(kit, "детайл", 10.dp.toPx(), size.height - 12.dp.toPx(), style = kit.strong.copy(color = Color(0xFF3B2410)))
    // Шаблон с прорез около лагера
    val gap = (d * s).toFloat() / 2 + 6.dp.toPx()
    val plateTop = y0 - (plate * s).toFloat()
    drawRect(kit.c.metal, Offset(0f, plateTop), Size(cx - gap, y0 - plateTop))
    drawRect(kit.c.metal, Offset(cx + gap, plateTop), Size(size.width - cx - gap, y0 - plateTop))
    label(kit, "шаблон", 10.dp.toPx(), plateTop - 10.dp.toPx())

    // Фрезер: стебло + конус (широк при върха)
    val shankW = (narrow * s).toFloat() * 0.9f
    drawRect(kit.muted, Offset(cx - shankW / 2, top), Size(shankW, coneTopY - top))
    val cone = Path().apply {
        moveTo(cx - (d * s).toFloat() / 2, tipY)
        lineTo(cx + (d * s).toFloat() / 2, tipY)
        lineTo(cx + (narrow * s).toFloat() / 2, coneTopY)
        lineTo(cx - (narrow * s).toFloat() / 2, coneTopY)
        close()
    }
    drawPath(cone, kit.c.metal.copy(alpha = 0.92f))
    drawPath(cone, kit.ink, style = Stroke(1.5.dp.toPx(), join = StrokeJoin.Round))
    drawLine(kit.c.axis, Offset(cx, top - 6.dp.toPx()), Offset(cx, tipY + 10.dp.toPx()), 1.dp.toPx(), pathEffect = dashed)

    // Размери
    val dimX = cx + (d * s).toFloat() / 2 + 20.dp.toPx()
    dimension(kit, Offset(dimX, y0), Offset(dimX, tipY), "${z.mm()} mm")
    label(kit, "Ø ${d.mm()}", cx - (d * s).toFloat() / 2 - 6.dp.toPx(), tipY, Anchor.End, kit.strong)
    profile?.diameterAtDepth?.let {
        label(kit, "Ø ${it.mm()} при шаблона", cx - gap - 6.dp.toPx(), y0 + 12.dp.toPx(), Anchor.End)
    }
    if (profile == null) label(kit, "схематичен профил", size.width - 8.dp.toPx(), top, Anchor.End)
}

// ─── 7. Посока на рязане ────────────────────────────────────────────────

/** 1: лек проход отдясно наляво по върховете; 2: навътре и навън между пръстите отляво надясно. */
@Composable
fun CutPathDiagram(finger: Double?, slot: Double?) = Diagram(250.dp) { kit ->
    val f = finger ?: 12.7
    val s = slot ?: 12.7
    val pad = 14.dp.toPx()
    val count = 6
    val pitch = (size.width - 2 * pad) / (count + 0.2f)
    val fw = (pitch * f / (f + s)).toFloat()
    val bodyTop = 14.dp.toPx()
    val tipY = 110.dp.toPx()
    val woodBottom = size.height - 52.dp.toPx()
    drawRect(kit.c.metalLight, Offset(pad, bodyTop), Size(size.width - 2 * pad, 22.dp.toPx()))
    fingers(kit, pad + (pitch - fw) / 2, bodyTop + 22.dp.toPx(), tipY, fw, pitch, 0, count - 1)
    drawRect(kit.c.wood, Offset(pad, tipY + 6.dp.toPx()), Size(size.width - 2 * pad, woodBottom - tipY - 6.dp.toPx()))

    // 1: от дясно наляво, почти докосвайки върховете
    val p1y = tipY + 18.dp.toPx()
    val p1Start = Offset(size.width - pad, p1y)
    val p1End = Offset(pad + 10.dp.toPx(), p1y)
    drawLine(kit.c.axis, p1Start, p1End, 3.dp.toPx(), cap = StrokeCap.Round)
    arrowHead(p1Start, p1End, kit.c.axis)

    // 2: навътре и навън между пръстите, отляво надясно
    val inY = bodyTop + 34.dp.toPx()
    val outY = woodBottom - 16.dp.toPx()
    val q = (pitch - fw) / 4
    val path = Path().apply {
        moveTo(pad + 4.dp.toPx(), outY)
        for (k in 0 until count - 1) {
            val slotCenter = pad + (k + 1) * pitch
            lineTo(slotCenter - q, outY)
            lineTo(slotCenter - q, inY)
            lineTo(slotCenter + q, inY)
            lineTo(slotCenter + q, outY)
        }
        lineTo(size.width - pad - 4.dp.toPx(), outY)
    }
    drawPath(path, kit.c.dimension, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    arrowHead(Offset(size.width - pad - 30.dp.toPx(), outY), Offset(size.width - pad - 4.dp.toPx(), outY), kit.c.dimension)

    // Легенда
    label(kit, "1 · лек проход отдясно наляво, по върховете", pad, size.height - 34.dp.toPx(), style = kit.strong.copy(color = kit.c.axis))
    label(kit, "2 · навътре и навън по пръстите, отляво надясно", pad, size.height - 12.dp.toPx(), style = kit.strong.copy(color = kit.c.dimension))
}
