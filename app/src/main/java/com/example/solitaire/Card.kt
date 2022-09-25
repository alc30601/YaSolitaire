package com.example.solitaire

import android.graphics.Color
import android.graphics.RectF

//--------------------------------------------------------------------------------------------------
// ハート、ダイヤ、クローバー、スペード
enum class Suit {
    Heart, Diamond, Clover, Spade
}

//--------------------------------------------------------------------------------------------------
// 表向き、裏向き
enum class Side {
    Front, Back
}

//--------------------------------------------------------------------------------------------------
class Card(val suit: Suit, val num: Int, var side: Side) {

    var rect: RectF = RectF()

    //----------------------------------------------------------------------------------------------
    // カードの表示座標矩形を設定する。
    fun setPosition(r:RectF) {
        rect = RectF(r)
    }

    //----------------------------------------------------------------------------------------------
    // 渡されたカードが自分と色違いかどうか確認する。
    // 色違い：ハートorダイヤに対してスペードorクローバ
    // 色違い：スペードorクローバに対してハートorダイヤ
    fun isDifferentColor(c1:Card):Boolean {
        return if (suit != Suit.Heart && suit != Suit.Diamond) {
            (c1.suit == Suit.Heart) || (c1.suit == Suit.Diamond)
        } else {
            (c1.suit == Suit.Spade) || (c1.suit == Suit.Clover)
        }
    }

    //----------------------------------------------------------------------------------------------
    // 渡されたカードが自分と同じマークかどうか確認する。
    fun isSameSuit(c1:Card):Boolean {
        return (suit == c1.suit)
    }

    //----------------------------------------------------------------------------------------------
    fun suitStr(): String {
        return when (suit) {
            Suit.Heart   -> "♥"
            Suit.Diamond -> "♦"
            Suit.Clover  -> "♣"
            Suit.Spade   -> "♠"
        }
    }

    //----------------------------------------------------------------------------------------------
    fun color(): Int {
        return when (suit) {
            Suit.Heart   -> Color.RED
            Suit.Diamond -> Color.RED
            Suit.Clover  -> Color.BLACK
            Suit.Spade   -> Color.BLACK
        }
    }

    //----------------------------------------------------------------------------------------------
    fun numStr(): String {
        return when (num) {
            11 -> "J"
            12 -> "Q"
            13 -> "K"
            else -> num.toString()
        }
    }

}


