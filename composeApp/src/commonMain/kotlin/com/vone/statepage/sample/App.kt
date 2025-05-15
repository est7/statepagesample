package com.vone.statepage.sample

import androidx.compose.runtime.Composable
import com.vone.statepage.sample.presention.ComposeContactsPage
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    ComposeContactsPage()
}


/*
            Button(onClick = { showContent = !showContent }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                }
            }

 */