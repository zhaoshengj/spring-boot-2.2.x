/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package smoketest.atmosphere;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atmosphere.config.managed.Decoder;
import org.atmosphere.config.managed.Encoder;
import org.atmosphere.config.service.Disconnect;
import org.atmosphere.config.service.ManagedService;
import org.atmosphere.config.service.Ready;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;

@ManagedService(path = "/chat")
public class ChatService {

	private final Log logger = LogFactory.getLog(ChatService.class);

	@Ready
	public void onReady(AtmosphereResource resource) {
		this.logger.info("Connected " + resource.uuid());
	}

	@Disconnect
	public void onDisconnect(AtmosphereResourceEvent event) {
		this.logger.info("Client " + event.getResource().uuid() + " disconnected ["
				+ (event.isCancelled() ? "cancelled" : "closed") + "]");
	}

	@org.atmosphere.config.service.Message(encoders = JacksonEncoderDecoder.class,
			decoders = JacksonEncoderDecoder.class)
	public Message onMessage(Message message) throws IOException {
		this.logger.info("Author " + message.getAuthor() + " sent message " + message.getMessage());
		return message;
	}

	public static class JacksonEncoderDecoder implements Encoder<Message, String>, Decoder<String, Message> {

		private final ObjectMapper mapper = new ObjectMapper();

		@Override
		public String encode(Message m) {
			try {
				return this.mapper.writeValueAsString(m);
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

		@Override
		public Message decode(String s) {
			try {
				return this.mapper.readValue(s, Message.class);
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

	}

}
