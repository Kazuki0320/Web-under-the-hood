# Terminal History Tips (zsh)

## 履歴から `curl` を検索する

```bash
history | grep curl
```

大文字小文字を無視:
```bash
history | grep -i curl
```

## 履歴番号で実行する

例: `1066` 番を再実行
```bash
!1066
```

実行せず表示だけ:
```bash
!1066:p
```

注意:
- `!$1066` は誤り
- `!$` は「直前コマンドの最後の引数」を展開する機能

## 逆方向検索（インタラクティブ）

1. `Ctrl + r`
2. `curl` と入力
3. `Ctrl + r` で候補を遡る
4. `Enter` で実行、`→` で編集して実行
