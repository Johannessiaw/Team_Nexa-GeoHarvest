package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.GeoHarvestMainApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GeoHarvestViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: GeoHarvestViewModel = viewModel()
        GeoHarvestMainApp(viewModel = viewModel)
      }
    }
  }
}
