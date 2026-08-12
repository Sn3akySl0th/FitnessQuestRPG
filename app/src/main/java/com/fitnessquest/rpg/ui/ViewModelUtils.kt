package com.fitnessquest.rpg.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.fitnessquest.rpg.AppContainer
import com.fitnessquest.rpg.FitQuestApp

val CreationExtras.appContainer: AppContainer
    get() = (this[APPLICATION_KEY] as FitQuestApp).container
