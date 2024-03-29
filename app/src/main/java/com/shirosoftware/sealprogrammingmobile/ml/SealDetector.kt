package com.shirosoftware.sealprogrammingmobile.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.shirosoftware.sealprogrammingmobile.domain.Seal
import javax.inject.Inject
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class SealDetector @Inject constructor(private val context: Context) {
    /**
     * runObjectDetection(bitmap: Bitmap)
     *      TFLite Object Detection function
     */
    fun runObjectDetection(bitmap: Bitmap, threshold: Float): List<DetectionResult> {
        val image = TensorImage.fromBitmap(bitmap)

        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setScoreThreshold(threshold)
            .build()
        val detector = ObjectDetector.createFromFileAndOptions(
            context, // the application context
            MODEL_FILE_NAME, // must be same as the filename in assets folder
            options
        )

        val results = detector.detect(image)

        val resultToDisplay = results.filter {
            // 大きすぎるものは除外
            it.boundingBox.right - it.boundingBox.left < bitmap.width / 2
        }.map {
            // Get the top-1 category and craft the display text
            val category = it.categories.first()
            val label = category.label
            val score = category.score

            // Create a data object to display the detection result
            DetectionResult(it.boundingBox, label, score)
        }
        
        // 左下から列ごとに並び替え
        return resultToDisplay.sortedWith(CompareResultPosition)
    }

    class CompareResultPosition {
        companion object : Comparator<DetectionResult> {
            override fun compare(result1: DetectionResult, result2: DetectionResult): Int {
                // 左右位置が重なったら上下で比較
                if ((result1.boundingBox.left > result2.boundingBox.left
                            && result1.boundingBox.left < result2.boundingBox.right)
                    || (result1.boundingBox.left < result2.boundingBox.right
                            && result1.boundingBox.right > result2.boundingBox.left)
                ) {
                    return (result2.boundingBox.top - result1.boundingBox.top).toInt()
                }

                // 上記以外は左右位置で比較
                return (result1.boundingBox.left - result2.boundingBox.left).toInt()
            }
        }
    }

    /**
     * drawDetectionResult(bitmap: Bitmap, detectionResults: List<DetectionResult>
     *      Draw a box around each objects and show the object's name.
     */
    fun drawDetectionResult(
        bitmap: Bitmap,
        detectionResults: List<DetectionResult>,
        showScore: Boolean,
    ): Bitmap {
        val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT

        detectionResults.withIndex().forEach {
            val seal = sealByLabel(it.value.label)

            // draw bounding box
            pen.color = Color.YELLOW
            pen.strokeWidth = 8F
            pen.style = Paint.Style.STROKE
            val box = it.value.boundingBox
            canvas.drawRect(box, pen)

            val tagSize = Rect(0, 0, 0, 0)
            val scoreSize = Rect(0, 0, 0, 0)

            // calculate the right font size
            pen.style = Paint.Style.FILL_AND_STROKE
            pen.color = Color.YELLOW
            pen.strokeWidth = 2F

            val text = "${it.index + 1}.${seal.text}"

            pen.textSize = MAX_FONT_SIZE
            pen.getTextBounds(text, 0, text.length, tagSize)
            val fontSize: Float = pen.textSize * box.width() / tagSize.width()

            // adjust the font size so texts are inside the bounding box
            if (fontSize < pen.textSize) pen.textSize = fontSize

            val textTop = box.top - (box.top - box.bottom) / 2 - (tagSize.height() / 2)

            var margin = (box.width() - tagSize.width()) / 2.0F
            if (margin < 0F) margin = 0F
            canvas.drawText(
                text, box.left + margin,
                textTop, pen
            )

            // スコア
            if (showScore) {
                val score = "${it.value.score.times(100).toInt()}%"
                pen.getTextBounds(score, 0, score.length, scoreSize)
                pen.textSize = box.width() / 5
                canvas.drawText(
                    score, box.left + (box.width() / 2) - (scoreSize.width() / 2),
                    textTop + tagSize.height() + 20, pen
                )
            }
        }
        return outputBitmap
    }

    private fun sealByLabel(label: String): Seal {
        return Seal.values().find { it.label == label } ?: Seal.Forward
    }

    companion object {
        private const val MODEL_FILE_NAME = "seals_model.tflite"
        private const val MAX_FONT_SIZE = 88f
    }
}
