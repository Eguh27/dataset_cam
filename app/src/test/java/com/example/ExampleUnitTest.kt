package com.example

import com.example.data.model.AspectRatioPreset
import com.example.data.model.ResolutionDimension
import com.example.data.model.ResolutionPresets
import com.example.data.model.ResolutionQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testAspectRatioPresets_dimensions() {
    val squareOptions = ResolutionPresets.getOptionsForAspect(AspectRatioPreset.SQUARE_1_1)
    assertTrue("Square options should contain at least 4 presets", squareOptions.size >= 4)
    assertEquals(224, squareOptions[0].width)
    assertEquals(224, squareOptions[0].height)

    val widescreenOptions = ResolutionPresets.getOptionsForAspect(AspectRatioPreset.WIDESCREEN_16_9)
    assertTrue("Widescreen options should contain at least 4 presets", widescreenOptions.size >= 4)
    assertEquals(426, widescreenOptions[0].width)
    assertEquals(240, widescreenOptions[0].height)
  }

  @Test
  fun testCustomDimension_evenNumbersEnforced() {
    val custom = ResolutionPresets.createCustomDimension(
      aspect = AspectRatioPreset.STANDARD_4_3,
      baseWidthOrHeight = 355 // odd number
    )
    // Custom width must be made even (354)
    assertEquals(354, custom.width)
    assertEquals(0, custom.height % 2)
    assertTrue(custom.storageSavingsPercent > 0)
  }

  @Test
  fun testDefaultDimensions_forEveryPreset() {
    for (aspect in AspectRatioPreset.entries) {
      val dimLite = ResolutionPresets.getDimension(aspect, ResolutionQuality.LITE)
      val dimBalanced = ResolutionPresets.getDimension(aspect, ResolutionQuality.BALANCED)
      val dimHigh = ResolutionPresets.getDimension(aspect, ResolutionQuality.HIGH)
      assertNotNull(dimLite)
      assertNotNull(dimBalanced)
      assertNotNull(dimHigh)
      assertTrue(dimHigh.estimatedKb > dimLite.estimatedKb)
    }
  }

  @Test
  fun testFolderStructureType_properties() {
    val imageFolder = com.example.data.model.FolderStructureType.CLASS_DIRECTORIES
    assertTrue(imageFolder.treeRepresentation.contains("images/"))

    val trainValTest = com.example.data.model.FolderStructureType.TRAIN_VAL_TEST_SPLIT
    assertTrue(trainValTest.treeRepresentation.contains("train/"))
    assertTrue(trainValTest.treeRepresentation.contains("val/"))
  }

  @Test
  fun testMetadataExportFormats() {
    val jsonFormat = com.example.data.model.MetadataExportFormat.JSON_STANDARD
    assertEquals("json", jsonFormat.extension)
    assertEquals("application/json", jsonFormat.mimeType)

    val csvFormat = com.example.data.model.MetadataExportFormat.CSV_PANDAS_PYTORCH
    assertEquals("csv", csvFormat.extension)
    assertEquals("text/csv", csvFormat.mimeType)
  }

  @Test
  fun testBlurStatus_classification() {
    val sharp = com.example.processing.BlurStatus.SHARP
    assertEquals("Tajam", sharp.shortLabel)
    assertEquals("✓", sharp.icon)

    val blurry = com.example.processing.BlurStatus.BLURRY
    assertEquals("Buram", blurry.shortLabel)
    assertEquals("⚠️", blurry.icon)

    val resultSharp = com.example.processing.BlurAnalysisResult(
      rawVariance = 180.0,
      score = 90,
      status = com.example.processing.BlurStatus.SHARP,
      isBlurry = false
    )
    assertEquals(false, resultSharp.isBlurry)
    assertEquals(90, resultSharp.score)

    val resultBlurry = com.example.processing.BlurAnalysisResult(
      rawVariance = 32.0,
      score = 25,
      status = com.example.processing.BlurStatus.BLURRY,
      isBlurry = true
    )
    assertEquals(true, resultBlurry.isBlurry)
    assertTrue(resultBlurry.score < 50)
  }
}
