sed -i '' -e '/val statsRow = @Composable {/,/^                }/d' "/Users/hakankorhasan/AndroidStudioProjects/brainland/app/src/main/java/com/example/brain_land/ui/games/tiltmaze/GameResultView.kt"
sed -i '' -e '/statsRow()/d' "/Users/hakankorhasan/AndroidStudioProjects/brainland/app/src/main/java/com/example/brain_land/ui/games/tiltmaze/GameResultView.kt"
sed -i '' -e '/@Composable/,$ { /private fun GameResultStatItem/,$d; }' "/Users/hakankorhasan/AndroidStudioProjects/brainland/app/src/main/java/com/example/brain_land/ui/games/tiltmaze/GameResultView.kt"
