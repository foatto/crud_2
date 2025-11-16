package foatto.server.service

import foatto.core.i18n.LanguageEnum
import foatto.server.SpringApp
import foatto.server.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class ChangeLanguageService(
    private val userRepository: UserRepository,
) {

    fun changeLanguage(
        sessionId: Long,
        lang: LanguageEnum,
    ) {
        SpringApp.getSessionData(sessionId)?.serverUserConfig?.let { serverUserConfig ->
            userRepository.findByIdOrNull(serverUserConfig.id)?.let { userEntity ->
                userEntity.lang = lang
                userRepository.saveAndFlush(userEntity)
            }
        }
    }
}