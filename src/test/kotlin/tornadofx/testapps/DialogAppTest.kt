package tornadofx.testapps

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.*
import tornadofx.*
import tornadofx.getValue
import tornadofx.setValue

class DialogAppTest: App(Main::class) {

    class Main: View() {
        private val model: UserDTOModel by inject()

        override val root = vbox {
            setPrefSize(300.00, 200.0)
            alignment = Pos.CENTER
            spacing = 10.0
            label(model.loggedInMessage)
            button("Login in without ViewModel").action(::login)
            button("Login in with ViewModel").action(::loginWithViewModel)
        }

        private fun login() {
            createLoginScreen().showAndWait().ifPresent {
                model.item = it
                model.commit()
            }
        }

        private fun loginWithViewModel() {
            createLoginScreenWithViewModel().showAndWait()
            model.commit()
        }

        private fun createLoginScreen(user: String = "", pass: String = ""): Dialog<UserDTO> {

            var userNameTF   by singleAssign<TextField>()
            var passwordTF   by singleAssign<PasswordField>()
            var rememberMeCB by singleAssign<CheckBox>()

            val loginBtnType = ButtonType("Login", ButtonBar.ButtonData.OK_DONE)
            val buttons = listOf(loginBtnType, ButtonType.CANCEL)

            return dialog("User Login", "Please enter login information", buttons) {
                form {
                    fieldset {
                        field("Username") {
                            userNameTF = textfield(user)
                        }
                        field("Password") {
                            passwordTF = passwordfield(pass)
                        }
                        field("Password") {
                            rememberMeCB = checkbox()
                        }
                    }
                }

                dialogPane.lookupButton(loginBtnType).apply {
                    disableWhen { userNameTF.textProperty().isEmpty and passwordTF.textProperty().isEmpty }
                }

                setResultConverter { btn ->
                    return@setResultConverter if(btn == loginBtnType) {
                        UserDTO(userNameTF.text, passwordTF.text, rememberMeCB.isSelected)
                    } else {
                        null
                    }
                }
            }
        }

        private fun createLoginScreenWithViewModel(): Dialog<UserDTO> {
            val loginBtnType = ButtonType("Login", ButtonBar.ButtonData.OK_DONE)
            val buttons = listOf(loginBtnType, ButtonType.CANCEL)

            return dialog("User Login", "Please enter login information", buttons) {
                form {
                    fieldset {
                        field("Username") {
                            textfield(model.username)
                        }
                        field("Password") {
                             passwordfield(model.password)
                        }
                        field {
                            checkbox("Remember Me", model.rememberMe)
                        }
                    }
                }

                dialogPane.lookupButton(loginBtnType).apply {
                    disableWhen { model.username.isBlank() and model.password.isBlank() }
                }
            }
        }
    }

    class UserDTO(username: String = "", password: String = "", rememberMe: Boolean = false) {
        val usernameProperty = SimpleStringProperty(username)
        var username by usernameProperty

        val passwordProperty = SimpleStringProperty(password)
        var password by passwordProperty

        val rememberMeProperty = SimpleBooleanProperty(rememberMe)
        var rememberMe by rememberMeProperty
    }

    class UserDTOModel(userDTO: UserDTO? = UserDTO()): ItemViewModel<UserDTO>(userDTO) {
        val username = bind(UserDTO::usernameProperty)
        val password = bind(UserDTO::passwordProperty)
        val rememberMe = bind(UserDTO::rememberMeProperty)

        val loggedInMessage = stringBinding(username) {  if(!value.isNullOrEmpty()) "User $value is logged in" else "No user is logged in"  }
    }

}