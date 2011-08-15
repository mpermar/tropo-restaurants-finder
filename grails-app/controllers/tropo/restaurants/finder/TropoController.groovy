package tropo.restaurants.finder

import org.apache.commons.io.IOUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import groovy.xml.MarkupBuilder;

import com.tropo.grails.TropoBuilder;

class TropoController implements ApplicationContextAware {

	ApplicationContext applicationContext
	
    def index = {
		
		def citiesFile = applicationContext.getResource('classpath:cities.txt').file
		def citiesString = new String(IOUtils.toByteArray(new FileInputStream(citiesFile)))
		def cities = citiesString.tokenize(',')
		
		def link = g.createLink(action:'places',absolute:true)
		def tropo = new TropoBuilder()
		tropo.tropo {
			say(value:"Bienvenido al servicio de información sobre restaurantes.", voice:"carmen")
			ask(name:'city',mode:"speech",recognizer:"es-es", voice:"carmen") {
				say(value:"Diga el nombre de una ciudad")
				choices(value:citiesString)
			}
			on(event:'continue',next:'/tropo/restaurant')
		}
		tropo.render(response)	
	}

	def restaurant = {
		
		def tropoRequest = request.JSON
		def place = tropoRequest.result.actions.value
	
		println "Found restaurant ${place}"
		def map = restaurantMap(place)
		session["restaurants"] = map
		def restaurants = restaurantList(map)
		
		def query = "http://api.11870.com/api/v2/search?appToken=29ea7f63c6f3a2bc8dacc3f6a9a3d84d&l=${place}&categoryOp=or&category=restaurantes"
		
		def tropo = new TropoBuilder()
		tropo.tropo {
			ask(name:'restaurante', mode:'speech', recognizer:'es-es', voice:'carmen') {
				say(value:"Diganos el nombre de un restaurante en ${place}")
				choices(value:restaurants)
			}
			on(event:'continue',next:'/tropo/info')
		}
		tropo.render(response)
	}
	
	def info = {

		
		def tropoRequest = request.JSON
		def restaurant = tropoRequest.result.actions.value

		def map = session["restaurants"]
		def phone = map.get(restaurant)
		
		def tropo = new TropoBuilder()
		tropo.tropo {
			say(value:"El teléfono de ${restaurant} es ${phone}. Gracias por utilizar nuestro servicio de información sobre restaurantes.", voice:"carmen")
			hangup()
		}
		tropo.render(response)		
	}
	
	public void setApplicationContext(ApplicationContext applicationContext) {
		
		this.applicationContext = applicationContext;
	}
	
	def restaurantMap(def place) {

		def query = "http://api.11870.com/api/v2/search?appToken=29ea7f63c6f3a2bc8dacc3f6a9a3d84d&l=${place}&categoryOp=or&category=restaurantes"
		
		def feed = new XmlParser().parse(query)

		def restaurants = []
		def map=[:]
		feed.entry.each {
			restaurants << it.title.text()
			map.put(it.title.text(),it.'oos:telephone'.text())
		}
		map
	}
	
	def restaurantList(def map) {
		
		map.keySet().join(",")
	}
}

