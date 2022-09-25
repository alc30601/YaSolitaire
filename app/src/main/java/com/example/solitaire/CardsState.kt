package com.example.solitaire

//import android.graphics.PointF
//import android.graphics.RectF
import android.util.Log


//--------------------------------------------------------------------------------------------------
class CardsState {

    // 札集合の種別
    enum class Kind {
        Stock,      // 山札(Stock)
        Discard,    // 捨札(Discard)
        Goal,       // ゴール(Goal)
        Field,      // 場(Field)
        Nothing     // 何でもない
    }

    val stock = ArrayDeque<Card>()                       // 山札(Stock)
    val discard = ArrayDeque<Card>()                     // 捨札(Discard)
    val goal = arrayOfNulls<ArrayDeque<Card>>(4)    // ゴール(Goal)
    val field = arrayOfNulls<ArrayDeque<Card>>(7)   // 場(Field)
    val moving = ArrayDeque<Card>()                      // 移動中

    var movingKind: Kind = Kind.Nothing                  // 移動中の場合にどこから移動したのかを表す
    var movingSrcCol: Int = 0                            // 移動元が場だった場合の移動元列

    var placeAbleKind: Kind = Kind.Nothing               // 移動中の移動先候補種別
    var placeAbleCol: Int = -1                           // 移動中の移動先候補列

    init {

        // 52枚のカードを生成する。
        val temporaryList:MutableList<Card> = mutableListOf()
        for (i in 1..13) {
            temporaryList.add( Card(Suit.Heart,  i, Side.Back) )
            temporaryList.add( Card(Suit.Diamond,i, Side.Back) )
            temporaryList.add( Card(Suit.Clover, i, Side.Back) )
            temporaryList.add( Card(Suit.Spade,  i, Side.Back) )
        }

        // 生成したカードをランダムな順番で場と山に登録する。
        // 場には7,6,5,4,3,2,1枚配布、残りは山に置く。
        temporaryList.shuffle()

        // 場にカードを配る。
        field[0] = createMutableList(temporaryList, 0,7)      // 7 cards
        field[1] = createMutableList(temporaryList, 7,13)     // 6 cards
        field[2] = createMutableList(temporaryList, 13,18)    // 5 cards
        field[3] = createMutableList(temporaryList, 18,22)    // 4 cards
        field[4] = createMutableList(temporaryList, 22,25)    // 3 cards
        field[5] = createMutableList(temporaryList, 25,27)    // 2 cards
        field[6] = createMutableList(temporaryList, 27,28)    // 1 cards

        // 場札の末尾のカードは表を向ける。
        field.forEach { it?.last()?.side  = Side.Front }

        // 残りのカードを山札に置く。
        val srcSub = temporaryList.subList(28, temporaryList.size)
        srcSub.forEach { this.stock.add(it) }

        // 捨札は空

        // ゴールは空
        for(i in 0..3   ) {
            goal[i] = ArrayDeque<Card>()
        }

        // 移動中は空

        Log.d("CardsState",  "init done")
    }

    //----------------------------------------------------------------------------------------------
    fun getTheArray(kind:Kind) : Array<ArrayDeque<Card>?>? {
        return when (kind) {
            Kind.Field -> field
            Kind.Goal -> goal
            else -> null
        }
    }

    //----------------------------------------------------------------------------------------------
    // srcListのfromIndexからtoIndexまでを切り出しArrayDequeに格納し戻り値として返す。
    private fun createMutableList(srcList: MutableList<Card>, fromIndex:Int, toIndex:Int): ArrayDeque<Card> {
        val temp = ArrayDeque<Card>()
        val srcSub = srcList.subList(fromIndex, toIndex)
        srcSub.forEach { temp.add(it) }
        return temp
    }

    //----------------------------------------------------------------------------------------------
    // 移動中カードが指定された種別、列に置くことができるか確認する。
    fun isAddableIn(col: Int, kind:Kind): Boolean {
        var result = false
        if(!moving.isEmpty()) {
            val c:Card = moving.first()
            result = when (kind) {
                Kind.Field -> {
                    isAddableInField(c, col)
                }
                Kind.Goal -> {
                    (moving.size == 1) && isAddableInGoal(c, col)
                }
                else -> {
                    false
                }
            }
        }
        return result
    }

    //----------------------------------------------------------------------------------------------
    // カードが場の指定された列に置けるか否かチェックする。
    // 以下の場合は置ける。
    // - 場のcol列にカードが１枚もなく、移動中カードの先頭が13(King)である。
    // - 場のcol列の一番上のカードが移動中カードの先頭の色違い数値が１つ大きい。
    // それ以外の場合は置けない。
    private fun isAddableInField(card:Card, col:Int): Boolean {
        return if(field[col]!!.isEmpty()){
            card.num == 13
        } else {
            val d:Card = field[col]!!.last()
            card.isDifferentColor(d) && (card.num + 1 == d.num)
        }
    }

    //----------------------------------------------------------------------------------------------
    // カードがゴールの指定された列に置けるか否かチェックする。
    // 以下の場合は置ける。
    //   - ゴールのcol列にカードが１枚もない && 移動中のカードの数値が1である。
    //   - ゴールのcol列の一番上のカードが移動中カードと同種 && 数値が１つ小さい。
    // それ以外の場合は置けない。
    private fun isAddableInGoal(card:Card, col:Int): Boolean {
        var result = false
        if(goal[col]!!.isEmpty()) {
            if(card.num == 1) {
                result = true
            }
        } else {
            val d:Card = goal[col]!!.last()
            result = card.isSameSuit(d) && (d.num + 1 == card.num)
        }
        return result
    }

    //----------------------------------------------------------------------------------------------
    // 場の指定された列のカードの一番上を表にする。
    // 但し既に表向きならば何もしない
    fun turnOverTopCardInFields(col:Int) {
        turnOverTopCard(field[col]!!)
    }

    //----------------------------------------------------------------------------------------------
    // 指定された配列のカードの一番上を表にする。
    // 但し既に表向きならば何もしない
    private fun turnOverTopCard(array: ArrayDeque<Card>) {
        if(!array.isEmpty()) {
            val card:Card = array.last()
            if(card.side == Side.Back) {
                card.side = Side.Front
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // カードの移動
    // srcのindexから末尾までのカードをdstの末尾に移行する。
    fun moveCards(src:ArrayDeque<Card>, dst:ArrayDeque<Card>, index:Int): Int {
        val numOfMoving:Int = src.size - index
        val temp = ArrayDeque<Card>()

        for(i in 1..numOfMoving) {
            temp.add(src.removeLast())
        }
        for(i in 1..numOfMoving) {
            dst.add(temp.removeLast())
        }
        return numOfMoving
    }

    //----------------------------------------------------------------------------------------------
    // カードの移動
    // srcの末尾のカードをdstの末尾に1枚移行する。
    // 戻り値として移動したカードを返す。(移動しなかった場合はnull)
    fun moveCard(src:ArrayDeque<Card>, dst:ArrayDeque<Card>): Card? {
        val card:Card? = src.removeLastOrNull()
        if(card != null) {
            dst.add(card)
        }
        return card
    }


    //----------------------------------------------------------------------------------------------
    // 山札のカードを１枚表にして捨て場に移動する。
    // 山札に一枚もカードがない場合は捨て場のカードを全て山札に戻す。
    fun actionInStock() {
        val card:Card? = moveCard(stock, discard)
        if(card != null){
            card.side = Side.Front
        } else {
            moveCards(discard, stock, 0)
            stock.forEach {
                it.side = Side.Back
            }
            stock.reverse() // = cs.discard.reversed()
        }

    }

    //----------------------------------------------------------------------------------------------
    fun autoOneStep() : Boolean{
        var done:Boolean
        done = autoField2Goal()
        if(!done)
            done = autoDiscard2Goal()
        return done
    }

    //----------------------------------------------------------------------------------------------
    // 自動で一手操作する。
    // 場のカードからゴールに移動可能であれば1枚移動する。
    // 移動できたらtrue, 出来なかったらfalseを返す。
    private fun autoField2Goal() : Boolean {
        var done = false
        for((_, f) in field.withIndex()) {
            done = auto2Goal(f!!)
            if(done) break
        }
        return done
    }

    //----------------------------------------------------------------------------------------------
    // 自動で一手操作する。
    // 捨て場の一番上のカードをゴールに移動できれば移動する。
    private fun autoDiscard2Goal() : Boolean {
        return auto2Goal(discard)
    }

    //----------------------------------------------------------------------------------------------
    // 自動で一手操作する。
    // 指定されたリストから一枚ゴールに移動できれば移動する。
    private fun auto2Goal(fromArray: ArrayDeque<Card>) : Boolean {
        var done = false
        if(!fromArray.isEmpty()) {
            val card = fromArray.last()
            for((gi, g) in goal.withIndex()) {
                if(isAddableInGoal(card, gi)) {
                    moveCard(fromArray, g!!)
                    turnOverTopCard(fromArray)
                    done = true
                    break
                }
            }
        }
        return done
    }

    //----------------------------------------------------------------------------------------------
    // 場の中で裏向きのカードの枚数を数える。
    fun countBackCardsInField() : Int {
        var count = 0
        field.forEach { col ->
            col!!.forEach { row ->
                if(row.side == Side.Back) { count++ }
            }
        }
        return count
    }

    //----------------------------------------------------------------------------------------------
    // ゲーム終了判定
    // ゴールのカード枚数が全て13に達していたら終了とみなす。
    fun isFinished() : Boolean {
        var numDone = 0
        goal.forEach {
            if(it!!.size == 13) {
                numDone++
            }
        }
        return numDone >= 4
    }
}