//Todo.
// - トランプ表示の可視化性の向上 ✔
//      - トランプ矩形の枠のゴミ解消 ✔
//      - トランプの数値判別容易性向上 ✔
// - 操作性の向上
//      - 山札位置の変更 ✔
//      - 移動カードリリース時の置き場所を置くことができる箇所優先とする。✔
//        可能な場所が隣り合う場合、intersectionの大きい方にする。✔
// - 完了判定処理の追加✔
// - AUTOボタンの実装✔
// - ゲーム結果の記録
// - ビルド警告の除去
// - アプリアイコンの作成

package com.example.solitaire

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    //----------------------------------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // New Gameボタン押下でゲーム初期化
        val btnNewGame = findViewById<Button>(R.id.button7)
        btnNewGame.setOnClickListener {
            val canvas = findViewById<BoardCanvas>(R.id.view)
            canvas.newGame()
        }

        // AUTOボタン
        val btnAuto = findViewById<Button>(R.id.button8)
        btnAuto.setOnClickListener {
            val canvas = findViewById<BoardCanvas>(R.id.view)
            canvas.autoExecute()
        }
    }
}

