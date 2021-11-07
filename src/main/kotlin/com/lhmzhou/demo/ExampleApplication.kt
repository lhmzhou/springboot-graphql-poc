package com.lhmzhou.demo

import com.coxautodev.graphql.tools.GraphQLMutationResolver
import com.coxautodev.graphql.tools.GraphQLQueryResolver

import org.slf4j.LoggerFactory

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.support.beans
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

import java.util.*

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

private val log = LoggerFactory.getLogger(ExampleApplication::class.java)

@SpringBootApplication
class ExampleApplication

fun main(args: Array<String>) {
    runApplication<ExampleApplication>(*args) {
        addInitializers(beans)
    }
}

val beans = beans {
    bean<SongService> {
        SongServiceImpl(ref())
    }
    bean<SongQuery>()
    bean<SongMutation>()
}

@Entity
data class Song(
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        val id: Long? = null,
        val publicId: String,
        var title: String,
        var writer: String
)

interface SongRepository : JpaRepository<Song, Long> {
    fun findByPublicId(publicId: String): Song
    fun existsByPublicId(publicId: String): Boolean
    fun deleteByPublicId(publicId: String)
}

interface SongService {
    fun getSong(publicId: String): Song
    fun getSongs(size: Int): List<Song>
    fun createSong(title: String, writer: String): Song
    fun deleteSong(publicId: String): String?
    fun updateSong(publicId: String, title: String, writer: String): Song
}

open class SongServiceImpl(private val SongRepository: SongRepository) : SongService {

    @Transactional(readOnly = true)
    override fun getSong(publicId: String) = songRepository.findByPublicId(publicId)

    @Transactional(readOnly = true)
    override fun getSongs(size: Int): List<Song> =
            songRepository.findAll(PageRequest.of(0, size, Sort.by(Sort.Direction.ASC, "id"))).content

    @Transactional
    override fun createSong(title: String, writer: String) = songRepository.save(Song(null, UUID.randomUUID().toString(), title, writer))

    @Transactional
    override fun updateSong(publicId: String, title: String, writer: String): Song {
        val song = songRepository.findByPublicId(publicId)
        song.writer = writer
        song.title = title
        return songRepository.save(song)
    }

    @Transactional
    override fun deleteSong(publicId: String): String? {
        if (songRepository.existsByPublicId(publicId)) {
            songRepository.deleteByPublicId(publicId)
            return publicId
        }
        return null
    }
}

class SongQuery(private val songService: SongService) : GraphQLQueryResolver {
    fun getSong(publicId: String) = songService.getSong(publicId)
    fun getSongs(size: Int) = songService.getSongs(size)
}

class SongMutation(private val songService: SongService) : GraphQLMutationResolver {
    fun createSong(title: String, writer: String) = songService.createSong(title, writer)
    fun updateSong(publicId: String, title: String, writer: String): Song = songService.updateSong(publicId, title, writer)
    fun deleteSong(publicId: String): String? = songService.deleteSong(publicId)
}