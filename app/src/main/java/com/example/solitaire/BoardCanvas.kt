// BoardCanvas.kt
// ♥♦♣♠
package com.example.solitaire

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.util.Log
import androidx.appcompat.app.AlertDialog


//--------------------------------------------------------------------------------------------------
class BoardCanvas(context: Context, attrs: AttributeSet): View(context, attrs) {

    private val p = Paint()

    // カード配置座標
    private var stockRect = RectF()                      // 山札(Stock)座標
    private var discardRect = RectF()                    // 捨札(Discard)座標
    private val goalRect = arrayOfNulls<RectF>(4)   // ゴール(Goal)座標
    private val fieldRect = arrayOfNulls<RectF>(7)  // 場(Field)座標

    // カード状態
    private var cs: CardsState = CardsState()

    // カード間の隙間(DP単位)
    private var marginX:Float = 20.0F
    private var marginY:Float = 20.0F

    // 場のカードのずらし幅
    private var marginField:Float = 100.0F

    // 何らかのイベント発生時の座標を記憶しておく。
    private var pos:PointF = PointF()

    // 場のカードのどの列の何枚目、を表すクラス(関数の受け渡しのためクラス化)
    class RowCol(var row:Int = 0, var col:Int = 0)
    class ColRect(var col:Int=0, var r:RectF = RectF())

    //----------------------------------------------------------------------------------------------
    init {}

    //----------------------------------------------------------------------------------------------
    // 新規ゲーム開始
    fun newGame() {
        this.cs = CardsState()

        invalidate()
    }

    //----------------------------------------------------------------------------------------------
    // 自動実行
    fun autoExecute() {
        autoExeHandler.postDelayed(runnable, autoStepPeriod)
    }

    val autoStepPeriod : Long = 100     // 自動実行の１手毎の間隔[ms]
    val autoExeHandler = Handler(Looper.getMainLooper())

    //----------------------------------------------------------------------------------------------
    private val runnable = object : Runnable {
        override fun run() {

            // もうゲームが完了していたら自動実行を終了する。
            if(cs.isFinished()) {
                autoExeHandler.removeCallbacks(this)
                notifyCompletion()
            } else {
                // 1回自動実行をやってみる
                val done: Boolean = cs.autoOneStep()

                // 1枚移動出来たらまだ移動できる可能背があるため、次のステップを仕掛ける。
                if (done) {
                    invalidate()
                    autoExeHandler.postDelayed(this, autoStepPeriod)

                // 1枚も移動できなかったが場に裏向きのカードがない場合は
                // 山札から1枚引いて次のステップを仕掛ける。
                } else if (cs.countBackCardsInField() <= 0) {
                    cs.actionInStock()
                    invalidate()
                    autoExeHandler.postDelayed(this, autoStepPeriod)

                // 1枚も移動できず、まだ場に裏向きのカードがある場合は自動実行を終了する。
                } else {
                    autoExeHandler.removeCallbacks(this)
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // ゲーム完成通知
    private fun notifyCompletion() {
        //---------------------------------------
        AlertDialog.Builder(context)
            .setTitle("Complete Notification")
            .setMessage("Congratulations!!")
            .setPositiveButton("Ok", null)
            .show()
        //---------------------------------------
    }

    //----------------------------------------------------------------------------------------------
    // ボードの幅からカードの幅(Pixel単位)を決める。
    // 高さは十分にあるので幅だけ考える。
    private fun calcCardWidth(): Int {
        val density = context.resources.displayMetrics.density
        val reqWidthAsDPI = calcCardWithAsDp()
        val reqWidthAsPX = reqWidthAsDPI / density
        return reqWidthAsPX.toInt()
    }

    //----------------------------------------------------------------------------------------------
    // カードの表示上の横幅(dp単位)を計算する。
    // Canvas上に7枚カードを少し隙間を取って並べられるぐらいのサイズ
    private fun calcCardWithAsDp() : Float {
        return width.toFloat() / 7 - marginX
    }

    //----------------------------------------------------------------------------------------------
    private fun calcBoardHeightAsDp() : Float {
        return height.toFloat()
    }

    //----------------------------------------------------------------------------------------------
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        calcCoordinate()
    }

    //----------------------------------------------------------------------------------------------
    // カードを配置する座標を計算する。
    private fun calcCoordinate() {

        // DP単位でのカードの縦横サイズを算出する。
        val reqWidth: Float = calcCardWithAsDp()

        val cardImgWidth = calcCardWidth()
        val cardImgHeight =  cardImgWidth * 1.5

        val reqHeight: Float = cardImgHeight.toFloat() * (reqWidth / cardImgWidth.toFloat())

        val cardWidth:Float  =  reqWidth
        val cardHeight:Float =  reqHeight

        val marginTopX:Float = marginX / 2
        val marginTopY:Float = marginY


//        山札、捨て場、ゴールを下に配置

        val bh = calcBoardHeightAsDp()
        val baseH:Float = bh - cardHeight - marginTopY

        var x:Float = marginTopX
        var y:Float = baseH

        // ゴールの表示座標矩形
        var r = RectF(x, y, x+cardWidth, y+cardHeight)
        for (i in 0..3) {
            this.goalRect[i] = RectF(r)
            r.offset(cardWidth+marginX, 0F)
        }

        // 捨て場の表示座標矩形
        this.discardRect = RectF(r)
        this.discardRect.offset(cardWidth+marginX, 0F)

        // 山札の表示座標矩形
        this.stockRect = RectF(this.discardRect)
        this.stockRect.offset(cardWidth+marginX, 0F)

        // 場(１枚目)の表示座標矩形
        x = marginTopX
        y = marginTopY
        r = RectF(x, y, x+cardWidth, y+cardHeight)
        for (i in 0..6) {
            this.fieldRect[i] = RectF(r)
            r.offset(+(cardWidth+marginX), 0F)
        }
    }


    //----------------------------------------------------------------------------------------------
    // 描画
    // 同時にカードの表示場所をカード状態として保持する。
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 緑のマット
        canvas.drawColor(Color.rgb(0, 100,0))

        // 山札描画
        if (!cs.stock.isEmpty()) {
            drawBack(canvas, stockRect)
        } else {
            drawGreen(canvas, stockRect)
        }
        // 山札に含まれるカードに表示座標矩形を設定
        cs.stock.forEach {
            it.setPosition(stockRect)
        }

        // 捨札描画
        if(!cs.discard.isEmpty()) {
            drawCardImage(canvas, cs.discard.last(), discardRect)
        } else {
            drawGreen(canvas, discardRect)
        }
        // 捨札に含まれるカードに表示座標矩形を設定
        cs.discard.forEach {
            it.setPosition(discardRect)
        }

        // ゴール描画
        for (i in 0..3) {
            if(!cs.goal[i]!!.isEmpty()) {
                drawCardImage(canvas, cs.goal[i]!!.last(), goalRect[i]!!)
            } else {
                drawGreen(canvas, goalRect[i]!!)
            }

            // ゴールに含まれるカードに表示座標矩形を設定
            cs.goal[i]!!.forEach {
                it.setPosition(goalRect[i]!!)
            }

        }

        // 場描画
        for (i in 0..6) {
            val r = RectF(fieldRect[i]!!)
            cs.field[i]!!.forEach {
                drawCardImage(canvas, it, r)
                it.setPosition(r)       // 場に含まれるカードに表示座標矩形を設定
                r.offset(0F, marginField)
            }
        }

        // 移動可能な場、ゴールがあれば強調表示
        if(!cs.moving.isEmpty()) {
            // 移動中カードを置ける場所の矩形を求める
            val r:RectF? =
            when (cs.placeAbleKind) {
                CardsState.Kind.Field -> {
                    if(cs.field[cs.placeAbleCol]!!.isEmpty()) { fieldRect[cs.placeAbleCol]!! }
                    else { cs.field[cs.placeAbleCol]!!.last().rect }
                }
                CardsState.Kind.Goal -> {
                    goalRect[cs.placeAbleCol]!!
                }
                else -> { null }
            }

            if(r != null) {
                // グレー半透明で該当領域を塗る
                val pColor = p.color
                p.color = Color.rgb(128, 128,64)
                p.color = Color.argb(128, 128, 128, 128)
                canvas.drawRect(r, p)
                p.color = pColor
            }

        }

        // 移動中描画
        if(!cs.moving.isEmpty()) {
            cs.moving.forEach {
                val r = it.rect
                drawCardImage(canvas, it, r)
            }
            Log.d("onDraw", "draw moving cards")
        }


/*
        //----------------------------------------------------
        // 画面サイズの確認用
        var p2 = Paint().apply {
            color = Color.CYAN
            strokeWidth = 10f
        }

        var x:Float = 0f
        var y:Float = 0f
        for (i in 0..107) {
            p2.color = if (i % 10 == 0) BLUE else Color.CYAN
            x = i.toFloat() * 10f
            y = i.toFloat() * 10f
            canvas.drawPoint(x, y, p2)
        }
        p2.color = Color.RED
        canvas.drawPoint(x, y, p2)
*/
    }

    //----------------------------------------------------------------------------------------------
    // トランプ配置場所の緑色を描く。
    private fun drawGreen(canvas:Canvas, r:RectF) {

        val pColor = p.color
        p.color = Color.rgb(64, 128,64)
        canvas.drawRect(r, p)
        p.color = pColor

    }

    //----------------------------------------------------------------------------------------------
    // カードの表示
    // 裏向きの場合は裏画像を表示
    // 表向きの場合は数字、マークの描画
    private fun drawCardImage(canvas:Canvas, card:Card, r:RectF) {

        if (card.side == Side.Front) {
            drawBigNumSuit(canvas, card, r)
        } else {
            drawBack(canvas, r)
        }
    }

    //----------------------------------------------------------------------------------------------
    // トランプ裏面を描く。
    private fun drawBack(canvas:Canvas, r:RectF) {

        // 塗りつぶす
        val pColor = p.color
        p.color = Color.rgb(128, 128,64)
        canvas.drawRect(r, p)

        // 縁を黒くする
        val pStyle = p.style
        val pStrokeWidth = p.strokeWidth
        p.style = Paint.Style.STROKE
        p.strokeWidth= 2F
        p.color = Color.rgb(100, 100,100)
        canvas.drawRect(r, p)
        p.strokeWidth = pStrokeWidth
        p.style = pStyle

        // なにか模様っぽいもの
        p.textSize = 20.0F
        p.color = Color.CYAN
        canvas.drawText("♥♣♦♠",r.left+20, r.top+40, p)
        canvas.drawText("♣♦♠♥",r.left+20, r.top+80, p)
        canvas.drawText("♦♠♥♣",r.left+20, r.top+120, p)
        canvas.drawText("♠♥♣♦",r.left+20, r.top+160, p)

        p.style = pStyle
        p.color = pColor
    }

    //----------------------------------------------------------------------------------------------
    // カード表面の描画を行う。
    // カード上部に数字とマーク、カード中央に大きくマークを描く
    private fun drawBigNumSuit(canvas:Canvas, card:Card, r:RectF) {

        // 先ずカードを白く塗りつぶす。
        val pColor = p.color
        p.color = Color.WHITE
        canvas.drawRect(r, p)
        p.color = pColor

        // 縁を黒くする
        val pStyle = p.style
        val pStrokeWidth = p.strokeWidth
        p.style = Paint.Style.STROKE
        p.strokeWidth= 2F
        p.color = Color.rgb(200, 200,200)
        canvas.drawRect(r, p)
        p.strokeWidth = pStrokeWidth
        p.style = pStyle


        // 数字とマークをカード上部に書く。
        val num = card.numStr()
        val suit = card.suitStr()
        val textSize = p.textSize
        p.textSize = 50.0F
        p.color = card.color()
        if(card.num == 10) {
            canvas.drawText("$num$suit", r.left + 10, r.top + 50, p)
        } else {
            canvas.drawText("$num $suit", r.left + 10, r.top + 50, p)
        }

        // 大きくマークをカード中央に書く。
        p.textSize = 80.0F
        canvas.drawText(suit, r.left+20, r.top+50+100, p)
        p.textSize = textSize
    }




    //----------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------


    //----------------------------------------------------------------------------------------------
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pt = PointF(event.x, event.y)
        when (event.action){
            MotionEvent.ACTION_DOWN -> {
                Log.d("onTouchEvent", "ACTION_DOWN")
                actionDown(pt)
            }
            MotionEvent.ACTION_MOVE -> {
                Log.d("onTouchEvent", "ACTION_MOVE")
                actionMove(pt)
            }
            MotionEvent.ACTION_UP   -> {
                Log.d("onTouchEvent", "ACTION_UP")
                actionUp(pt)
            }
        }
        pos.set(event.x, event.y) // action*()で前回値を使うことがあるためaction_*()後に更新

        invalidate()
        return true
    }

    //----------------------------------------------------------------------------------------------
    // タップ時の処理
    private fun actionDown(pt:PointF) {
        var done:Boolean

        done = actionDownInFields(pt)
        if(done) return

        done = actionDownInStock(pt)
        if(done) return

        done = actionDownInDiscard(pt)
        if(done) return

        done = actionDownInGoal(pt)
        if(done) return

        actionDownInOthers(pt)
    }

    //----------------------------------------------------------------------------------------------
    // アンタップ時の処理
    private fun actionUp(pt:PointF) {
        var done:Boolean

        done = actionUpInFields(pt)
        if(done) return

        done = actionUpInStock(pt)
        if(done) return

        done = actionUpInDiscard(pt)
        if(done) return

        done = actionUpInGoal(pt)
        if(done){
            if(cs.isFinished()) {
                notifyCompletion()
            }
            return
        }

        actionUpInOthers(pt)
    }

    //----------------------------------------------------------------------------------------------
    // 移動イベント処理
    // 保持しているカードがあれば、タップ位置に応じて移動させる。
    // 保持しているカードがなければ何もしない。
    private fun actionMove(pt:PointF) {
        if(!cs.moving.isEmpty()) {
            val xOffset = pt.x - pos.x
            val yOffset = pt.y - pos.y
            cs.moving.forEach {
                it.rect.offset(xOffset, yOffset)
            }

            // 移動中の処理
            // カードを場またはゴールに持っていったときのみ
            var done:Boolean
            done = actionMoveInFields(pt)
            if(done) return

            done = actionMoveInGoal(pt)
            if(done) return

            actionMoveInOthers(pt)
        }
    }


    //----------------------------------------------------------------------------------------------
    // 場でタップされた
    private fun actionDownInFields(pt:PointF):Boolean {

        // 場のカードのタップチェック
        val rowCol = RowCol()

        // タップ時は移動中のカードが配置場所にあるか否かを判定する。
        val tappedP = PointF(pt)
        fun checkInRect(r: RectF): Boolean {
            return r.contains(tappedP.x, tappedP.y)
        }
        val isTapped:Boolean = checkInFields(::checkInRect, rowCol)

        // 場のカードがタップされたら、カードを場から"移動中"に移す。
        if(isTapped && rowCol.row >= 0) {
            val card = cs.field[rowCol.col]!![rowCol.row]
            Log.d("retrieveTappedCard", ""+card.suit+":"+card.num)

            // 裏向きの場のカードは選べない。
            if (card.side == Side.Back){
                return true
            }
            cs.moveCards(cs.field[rowCol.col]!!, cs.moving, rowCol.row)
            cs.movingSrcCol = rowCol.col
            cs.movingKind = CardsState.Kind.Field
            return true
        }
        return false
    }

    //----------------------------------------------------------------------------------------------
    // ゴールでタップされた
    private fun actionDownInGoal(pt:PointF):Boolean {
        var result = false
        val rowCol = RowCol()

        // タップ時は移動中のカードが配置場所にあるか否かを判定する。
        val tappedP = PointF(pt)
        fun checkInRect(r: RectF): Boolean {
            return r.contains(tappedP.x, tappedP.y)
        }
        val isTapped:Boolean = checkInGoals(::checkInRect, rowCol)

        if (isTapped) {
            val card:Card? = cs.moveCard(cs.goal[rowCol.col]!!, cs.moving)
            result = true
            if(card != null) {
                cs.movingKind = CardsState.Kind.Goal
                cs.movingSrcCol = rowCol.col
            }
        }
        return result
    }

    //----------------------------------------------------------------------------------------------
    // 山札でタップされた
    // 山札でタップされたときはアンタップが山札だった場合は１枚を捨て場に移動する。
    // ここでは山札でタップされたことを覚えておく。
    private fun actionDownInStock(pt:PointF):Boolean {
        val isTapped:Boolean = stockRect.contains(pt.x, pt.y)
        if(isTapped) {
            cs.movingKind = CardsState.Kind.Stock
        }
        return isTapped
    }

    //----------------------------------------------------------------------------------------------
    // 捨て場でタップされた
    private fun actionDownInDiscard(pt:PointF):Boolean {
        val isTapped:Boolean = discardRect.contains(pt.x, pt.y)
        if (isTapped) {
            val card:Card? = cs.moveCard(cs.discard, cs.moving)
            if(card != null) {
                cs.movingKind = CardsState.Kind.Discard
            }
             return true
        }
        return false
    }


    private fun actionDownInOthers(@Suppress("UNUSED_PARAMETER") pt:PointF) { }


    //----------------------------------------------------------------------------------------------
    // 場でアンタップされた
    // アンタップ処理
    // 掴んでいるカードがある場合、離す。アンタップ地点に移動させるか、元に戻す。
    private fun actionUpInFields(@Suppress("UNUSED_PARAMETER") pt:PointF):Boolean {
        var result = false
        if(!cs.moving.isEmpty()) {

            val movingRect:RectF = cs.moving.first().rect
            val index:Int = checkInTheKind(movingRect, CardsState.Kind.Field)
            if(index > -1) {

                // カードを置くことのできるところでアンタップされたので、移動中カードを移動させる。
                cs.moveCards(cs.moving, cs.field[index]!!, 0)

                // カードを移動したので元あったところの一番上のカードを表にする。
                if (cs.movingKind == CardsState.Kind.Field) {
                    cs.turnOverTopCardInFields(cs.movingSrcCol)
                }

                cs.movingKind = CardsState.Kind.Nothing
                cs.placeAbleKind = CardsState.Kind.Nothing
                result = true
            }
        }
        return result
    }

    //----------------------------------------------------------------------------------------------
    // ゴールでアンタップされた
    private fun actionUpInGoal(@Suppress("UNUSED_PARAMETER") pt:PointF):Boolean {

        var result = false
        if(cs.moving.size == 1) {  // ゴールに移動できるのは１枚づつ

            // アンタップ時は移動中のカードが配置場所に少しでも重なっていたらOKとする。
            val movingRect:RectF = cs.moving.first().rect

            val index:Int = checkInTheKind(movingRect, CardsState.Kind.Goal)
            if(index > -1) {
                cs.moveCard(cs.moving, cs.goal[index]!!)

                if (cs.movingKind == CardsState.Kind.Field) {
                    // カードを移動したので元あったところの一番上のカードを表にする。
                    cs.turnOverTopCardInFields(cs.movingSrcCol)
                }
                result = true
                cs.movingKind = CardsState.Kind.Nothing
            }
        }
        return result
    }


    //----------------------------------------------------------------------------------------------
    // 山札でアンタップされた
    // 山札でタップされて、山札でアンタップされた場合、山札を選択したものとみなす。
    private fun actionUpInStock(pt:PointF):Boolean {

        var result = false
        if (cs.movingKind == CardsState.Kind.Stock) {

            val isUnTapped:Boolean = stockRect.contains(pt.x, pt.y)
            if(isUnTapped) {

                // 山札のカードを１枚表にして捨て場に移動する。
                // 山札に一枚もカードがない場合は捨て場のカードを全て山札に戻す。
                cs.actionInStock()
                result = true
                cs.movingKind = CardsState.Kind.Nothing
            }
        }
        return result
    }

    //----------------------------------------------------------------------------------------------
    // 捨て札場でアンタップされた
    private fun actionUpInDiscard(@Suppress("UNUSED_PARAMETER") pt:PointF):Boolean {
        return false
    }

    //----------------------------------------------------------------------------------------------
    // カードを置けないところに置かれたのでもと合った場所に戻す。
    private fun actionUpInOthers(@Suppress("UNUSED_PARAMETER") pt:PointF) {

        if(!cs.moving.isEmpty()) {
            when (cs.movingKind) {
                CardsState.Kind.Field -> {
                    cs.moveCards(cs.moving, cs.field[cs.movingSrcCol]!!, 0)
                }
                CardsState.Kind.Goal -> {
                    cs.moveCard(cs.moving, cs.goal[cs.movingSrcCol]!!)
                }
                CardsState.Kind.Discard -> {
                    cs.moveCard(cs.moving, cs.discard)
                }
                else -> {}
            }
        }
        cs.movingKind = CardsState.Kind.Nothing
    }



    //----------------------------------------------------------------------------------------------
    // 移動中カードが場の上空
    private fun actionMoveInFields(@Suppress("UNUSED_PARAMETER") pt:PointF): Boolean {
        var result = false
        if(!cs.moving.isEmpty()) {

            // 移動中のカードが配置場所に少しでも重なっていたらOKとする。
            // 配置可能場所がonDraw時に色つけされるようにCardStateに情報を保持しておく。
            val movingRect:RectF = cs.moving.first().rect
            val index:Int = checkInTheKind(movingRect, CardsState.Kind.Field)
            if(index > -1) {
                cs.placeAbleKind = CardsState.Kind.Field
                cs.placeAbleCol = index
                result = true
            }
        }
        return result
    }

    //----------------------------------------------------------------------------------------------
    // 移動中カードがゴールの上空
    private fun actionMoveInGoal(@Suppress("UNUSED_PARAMETER") pt:PointF): Boolean {
        var result = false
        if(cs.moving.size == 1) {  // ゴールに移動できるのは１枚づつ

            val movingRect:RectF = cs.moving.first().rect
            val index:Int = checkInTheKind(movingRect, CardsState.Kind.Goal)
            if(index > -1) {
                cs.placeAbleKind = CardsState.Kind.Goal
                cs.placeAbleCol = index
                result = true
            }

        }
        return result
    }


    //----------------------------------------------------------------------------------------------
    // 移動中カードがどこでもない
    private fun actionMoveInOthers(@Suppress("UNUSED_PARAMETER") pt:PointF): Boolean {
        cs.placeAbleKind = CardsState.Kind.Nothing
        cs.placeAbleCol = -1
        return false
    }



    //----------------------------------------------------------------------------------------------
    // 引数の点が場のカードの何れかの列の上にあるか否か。
      private fun checkInFields(checkFun:(RectF) -> Boolean, rc:RowCol) : Boolean {
        var isTapped = false
        var col = 0
        var row = 0

        // 場の列を見て回る
        for((i, it) in cs.field.withIndex()) {
            // 場の特定列のカードを見て回る
//            val cardNum = cs.field[i]!!.size
            val cardNum = it!!.size

            // １枚以上のカードがある列
            if(cardNum > 0) {
                for (j in (cardNum - 1) downTo 0) {

                    val cardRect = cs.field[i]!![j].rect
                    isTapped = checkFun(cardRect)
                        if (isTapped) {
                            col = i
                            row = j
                        break
                    }
                }
                if (isTapped) {
                    break
                }
            }
            // 1枚もカードのない列
            else {
                isTapped = checkFun(fieldRect[i]!!)
                if(isTapped) {
                    col = i
                    row = -1    // カードがない場合何番目というのは意味がないので-1とする。
                    break
                }
            }
        }
        rc.col = col
        rc.row = row
        return isTapped
    }

    //----------------------------------------------------------------------------------------------
    // 引数の点がゴールのカードの何れかの列の上にあるか否か。
    private fun checkInGoals(checkFun:(RectF) -> Boolean, rc:RowCol) : Boolean{

        var isTapped = false
        var col = 0

        // 場の列を見て回る
        for((i, _) in cs.goal.withIndex()) {
            isTapped = checkFun(goalRect[i]!!)
            if(isTapped) {
                col = i
                break
            }
        }
        rc.col = col
        return isTapped
    }

    //----------------------------------------------------------------------------------------------
    // 引数矩形が場のカードの何れかの列に置けるか。置ける列の中で最も重なりの大きい列を選ぶ。
     private fun checkInTheKind(r:RectF, kind:CardsState.Kind) : Int {

        val array = cs.getTheArray(kind)
        val addAbles = ArrayDeque<ColRect>()

        // 場の列を見て回る
        // 置くことができる列の一覧を作る。
        for ((i, _) in array!!.withIndex()) {
            val colRect: RectF = mergeRect(kind, i)
            val rTest = RectF(r)
            val isOverlapped: Boolean = rTest.intersect(colRect)
            if (isOverlapped && cs.isAddableIn(i, kind)) {
                addAbles.add(ColRect(i, rTest))
            }
        }

        // 置くことのできる列一覧から最も置く可能性が高いものを選ぶ。
        // 重複面積が最も大きいものを選択する。
        var basisArea = 0.0F
        var basisIndex: Int = -1
        addAbles.forEach {
            val area:Float = areaRect(it.r)
            if(basisArea <= area) {
                basisArea = area
                basisIndex = it.col
            }
        }
        return basisIndex

    }





    //----------------------------------------------------------------------------------------------
    // kind種別のカードの専有する矩形領域を算出する。
    private fun mergeRect(kind: CardsState.Kind, index:Int): RectF {

        val occupiedArea: RectF
        when (kind) {
            // [場] 場が空だったら基準位置矩形、カードがあればマージ
            CardsState.Kind.Field -> {
                val r = RectF(fieldRect[index])
                if (!cs.field[index]!!.isEmpty()) {
                    cs.field[index]!!.forEach {
                        if(r.top > it.rect.top) r.top = it.rect.top
                        if(r.left > it.rect.left) r.left = it.rect.left
                        if(r.bottom < it.rect.bottom) r.bottom = it.rect.bottom
                        if(r.right < it.rect.right) r.right = it.rect.right
                    }
                }
                occupiedArea = RectF(r)
            }
            // [ゴール] 基準位置矩形
            CardsState.Kind.Goal -> {
                occupiedArea = RectF(goalRect[index])
            }
            // 場、ゴール以外でこの関数が呼ばれない想定
            else -> {
                occupiedArea = RectF()
            }
        }
        return occupiedArea
    }

    //----------------------------------------------------------------------------------------------
    // 矩形の面積を求める。
    private fun areaRect(r:RectF): Float {
        return (r.bottom - r.top) * (r.right - r.left)
    }
}

