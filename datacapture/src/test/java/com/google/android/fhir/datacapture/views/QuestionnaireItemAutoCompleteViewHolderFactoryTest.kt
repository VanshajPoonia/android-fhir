/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.fhir.datacapture.views

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.get
import com.google.android.fhir.datacapture.R
import com.google.android.fhir.datacapture.displayString
import com.google.android.fhir.datacapture.validation.Invalid
import com.google.android.fhir.datacapture.validation.NotValidated
import com.google.android.fhir.datacapture.validation.Valid
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import com.google.common.truth.Truth.assertThat
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class QuestionnaireItemAutoCompleteViewHolderFactoryTest {
  private val parent =
    FrameLayout(
      RuntimeEnvironment.getApplication().apply { setTheme(R.style.Theme_Material3_DayNight) }
    )
  private val viewHolder = QuestionnaireItemAutoCompleteViewHolderFactory.create(parent)

  @Test
  fun shouldSetQuestionHeader() {
    viewHolder.bind(
      QuestionnaireItemViewItem(
        Questionnaire.QuestionnaireItemComponent().apply { text = "Question" },
        QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _ -> },
      )
    )

    assertThat(viewHolder.itemView.findViewById<TextView>(R.id.question).text.toString())
      .isEqualTo("Question")
  }

  @Test
  fun shouldHaveSingleAnswerChip() {
    val questionnaireItem =
      Questionnaire.QuestionnaireItemComponent().apply {
        repeats = false
        addAnswerOption(
          Questionnaire.QuestionnaireItemAnswerOptionComponent()
            .setValue(Coding().setCode("test1-code").setDisplay("Test1 Code"))
        )
        addAnswerOption(
          Questionnaire.QuestionnaireItemAnswerOptionComponent()
            .setValue(Coding().setCode("test2-code").setDisplay("Test2 Code"))
        )
      }
    viewHolder.bind(
      QuestionnaireItemViewItem(
        questionnaireItem,
        QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
          addAnswer(
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
              value =
                questionnaireItem.answerOption
                  .first { it.displayString == "Test1 Code" }
                  .valueCoding
            }
          )
        },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _ -> },
      )
    )

    assertThat(viewHolder.itemView.findViewById<ChipGroup>(R.id.chipContainer).childCount)
      .isEqualTo(1)
  }

  @Test
  fun shouldHaveTwoAnswerChipWithExternalValueSet() {
    val answers =
      listOf(
        Questionnaire.QuestionnaireItemAnswerOptionComponent()
          .setValue(Coding().setCode("test1-code").setDisplay("Test1 Code")),
        Questionnaire.QuestionnaireItemAnswerOptionComponent()
          .setValue(Coding().setCode("test2-code").setDisplay("Test2 Code"))
      )
    val questionnaireItem =
      Questionnaire.QuestionnaireItemComponent().apply {
        repeats = true
        answerValueSet = "http://answwer-value-set-url"
      }
    viewHolder.bind(
      QuestionnaireItemViewItem(
        questionnaireItem,
        QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
          addAnswer(
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
              value = answers.first { it.displayString == "Test1 Code" }.valueCoding
            }
          )

          addAnswer(
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
              value = answers.first { it.displayString == "Test2 Code" }.valueCoding
            }
          )
        },
        resolveAnswerValueSet = {
          if (it == "http://answwer-value-set-url") {
            answers
          } else {
            emptyList()
          }
        },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _ -> },
      )
    )

    assertThat(viewHolder.itemView.findViewById<ChipGroup>(R.id.chipContainer).childCount)
      .isEqualTo(2)
  }

  @Test
  fun shouldHaveSingleAnswerChipWithContainedAnswerValueSet() {
    val answers =
      listOf(
        Questionnaire.QuestionnaireItemAnswerOptionComponent()
          .setValue(Coding().setCode("test1-code").setDisplay("Test1 Code")),
        Questionnaire.QuestionnaireItemAnswerOptionComponent()
          .setValue(Coding().setCode("test2-code").setDisplay("Test2 Code"))
      )
    viewHolder.bind(
      QuestionnaireItemViewItem(
        Questionnaire.QuestionnaireItemComponent().apply {
          repeats = false
          answerValueSet = "#ContainedValueSet"
        },
        QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
          addAnswer(
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
              value = answers.first { it.displayString == "Test1 Code" }.valueCoding
            }
          )
        },
        resolveAnswerValueSet = {
          if (it == "#ContainedValueSet") {
            answers
          } else {
            emptyList()
          }
        },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _ -> },
      )
    )

    assertThat(viewHolder.itemView.findViewById<ChipGroup>(R.id.chipContainer).childCount)
      .isEqualTo(1)
  }

  @Test
  fun displayValidationResult_error_shouldShowErrorMessage() {
    viewHolder.bind(
      QuestionnaireItemViewItem(
        Questionnaire.QuestionnaireItemComponent().apply { required = true },
        QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = Invalid(listOf("Missing answer for required field.")),
        answersChangedCallback = { _, _, _ -> },
      )
    )

    assertThat(viewHolder.itemView.findViewById<TextView>(R.id.error).visibility)
      .isEqualTo(View.VISIBLE)
    assertThat(viewHolder.itemView.findViewById<TextView>(R.id.error).text)
      .isEqualTo("Missing answer for required field.")
    assertThat(viewHolder.itemView.findViewById<TextInputLayout>(R.id.text_input_layout).error)
      .isNotNull()
  }

  @Test
  fun displayValidationResult_noError_shouldShowNoErrorMessage() {
    viewHolder.bind(
      QuestionnaireItemViewItem(
        Questionnaire.QuestionnaireItemComponent().apply {
          required = true
          addAnswerOption(
            Questionnaire.QuestionnaireItemAnswerOptionComponent().apply {
              value = Coding().apply { display = "display" }
            }
          )
        },
        QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
          addAnswer(
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
              value = Coding().apply { display = "display" }
            }
          )
        },
        validationResult = Valid,
        answersChangedCallback = { _, _, _ -> },
      )
    )

    assertThat(viewHolder.itemView.findViewById<TextView>(R.id.error).visibility)
      .isEqualTo(View.GONE)
    assertThat(viewHolder.itemView.findViewById<TextInputLayout>(R.id.text_input_layout).error)
      .isNull()
  }

  @Test
  fun bind_readOnly_shouldDisableView() {
    val questionnaireItem =
      Questionnaire.QuestionnaireItemComponent().apply {
        readOnly = true
        addAnswerOption(
          Questionnaire.QuestionnaireItemAnswerOptionComponent().apply {
            value = Coding().apply { display = "readOnly" }
          }
        )
      }
    viewHolder.bind(
      QuestionnaireItemViewItem(
        questionnaireItem,
        QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
          addAnswer(
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
              value =
                questionnaireItem.answerOption.first { it.displayString == "readOnly" }.valueCoding
            }
          )
        },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _ -> },
      )
    )

    assertThat(viewHolder.itemView.findViewById<ChipGroup>(R.id.chipContainer)[0].isEnabled)
      .isFalse()
  }
}
