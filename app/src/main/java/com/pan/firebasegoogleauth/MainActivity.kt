package com.pan.firebasegoogleauth

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.pan.firebasegoogleauth.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    // сутність, для роботи з Firebase
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnSignIn.setOnClickListener{
            // це небхідний набір параметрів, які необхідні для запуску актівіті з авторизацією через Google
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.webClient_id))
                .requestEmail()
                .build()
            // для виклику процесу авторизації
            val signInClient = GoogleSignIn.getClient(this, options)
            // запускаємо нову актівіті (в ній виведе те вікно, з варіантом запуску через аккаунт гугла)
            signInClient.signInIntent.also {
                // це новий (не deprecated) варіант, як запускати активіті. Запускаємо отриманий з signInClient Інтент
                getResult.launch(it)
            }
        }
    }


    private fun googleAuthForFirebase(account: GoogleSignInAccount) {
        // для запуску інтенту, гуглу треба отримати реквізити аккаунту
        // (я так розумію, це те поле, яке юзер просто клацає і заходить через обраний аккаунт)
        // якщо ніякого аккаунту немає, то в інтенті виведе запит ввести email та пароль від свого гугловського акку
        val credentials = GoogleAuthProvider.getCredential(account.idToken, null)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // await ждет, пока не закончится задача
                auth.signInWithCredential(credentials).await()
                withContext(Dispatchers.Main){
                    Toast.makeText(this@MainActivity, "Successfully logged in", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception){
                // если вдруг ловим ошибку, то withContext меняет тред на Main, чтоб можно бьіло вьівести Toast
                withContext(Dispatchers.Main){
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private val getResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        // в туториале говорилось, что resultCode хреново работает, но видимо через registerForActivityResult уже все норм
        if (it.resultCode == RESULT_OK){
            // отримуємо данні з переданого інтенту
            val account = GoogleSignIn.getSignedInAccountFromIntent(it.data).result
            account?.let {
                googleAuthForFirebase(it)
            }
        }
    }
}