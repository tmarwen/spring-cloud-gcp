/*
 * Copyright 2017-2018 the original author or authors.
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

package com.google.cloud.spring.stream.binder.pubsub;

import java.io.IOException;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubConfiguration;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.DefaultPublisherFactory;
import com.google.cloud.spring.pubsub.support.DefaultSubscriberFactory;
import com.google.cloud.spring.stream.binder.pubsub.properties.PubSubConsumerProperties;
import com.google.cloud.spring.stream.binder.pubsub.properties.PubSubExtendedBindingProperties;
import com.google.cloud.spring.stream.binder.pubsub.properties.PubSubProducerProperties;
import com.google.cloud.spring.stream.binder.pubsub.provisioning.PubSubChannelProvisioner;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import org.springframework.cloud.stream.binder.AbstractTestBinder;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Tests the binder for Pub/Sub.
 *
 * @author João André Martins
 * @author Daniel Zou
 */
public class PubSubTestBinder extends AbstractTestBinder<PubSubMessageChannelBinder,
		ExtendedConsumerProperties<PubSubConsumerProperties>,
		ExtendedProducerProperties<PubSubProducerProperties>> {

	public PubSubTestBinder(String host) {
		GcpProjectIdProvider projectIdProvider = () -> "porto sentido";

		// Transport channel provider so that test binder talks to emulator.
		ManagedChannel channel = ManagedChannelBuilder
				.forTarget(host)
				.usePlaintext()
				.build();
		TransportChannelProvider transportChannelProvider =
				FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));

		PubSubChannelProvisioner pubSubChannelProvisioner;

		try {
			SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(
					SubscriptionAdminSettings.newBuilder()
							.setTransportChannelProvider(transportChannelProvider)
							.setCredentialsProvider(NoCredentialsProvider.create())
							.build()
			);

			TopicAdminClient topicAdminClient = TopicAdminClient.create(
					TopicAdminSettings.newBuilder()
							.setTransportChannelProvider(transportChannelProvider)
							.setCredentialsProvider(NoCredentialsProvider.create())
							.build()
			);

			pubSubChannelProvisioner = new PubSubChannelProvisioner(
					new PubSubAdmin(projectIdProvider, topicAdminClient, subscriptionAdminClient)
			);
		}
		catch (IOException ioe) {
			throw new RuntimeException("Couldn't build test binder.", ioe);
		}
		PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
		DefaultSubscriberFactory subscriberFactory = new DefaultSubscriberFactory(projectIdProvider, pubSubConfiguration);
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		subscriberFactory.setGlobalScheduler(scheduler);
		subscriberFactory.setChannelProvider(transportChannelProvider);
		subscriberFactory.setCredentialsProvider(NoCredentialsProvider.create());

		DefaultPublisherFactory publisherFactory = new DefaultPublisherFactory(projectIdProvider);
		publisherFactory.setChannelProvider(transportChannelProvider);
		publisherFactory.setCredentialsProvider(NoCredentialsProvider.create());

		PubSubTemplate pubSubTemplate = new PubSubTemplate(publisherFactory, subscriberFactory);

		PubSubMessageChannelBinder binder =
				new PubSubMessageChannelBinder(null, pubSubChannelProvisioner, pubSubTemplate,
						new PubSubExtendedBindingProperties());
		GenericApplicationContext context = new GenericApplicationContext();
		binder.setApplicationContext(context);
		this.setBinder(binder);
	}

	@Override
	public void cleanup() {

	}
}
