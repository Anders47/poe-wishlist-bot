# PoE Wishlist Bot

## Description

A hobby project by a PoE player and computer science student, building a Discord bot in Java that monitors public stash tabs in a private Path of Exile league and notifies users when desired unique items appear.

## How to use
Once the bot is invited to the designated server and channel, you can manage your Discord-user specific wishlist with:
- !add Shavronne's Wrappings
- !remove Wanderlust
- !clearwishlist
- !listwishlist

## Background

On release of patch 3.26 for Path of Exile 1, I am going to (at the time of writing) be playing a small private league with some friends and friends of friends. Due to itemisation restrictions this would impose, especially in regards to uniques, some builds would likely be searching for their respective items among the players. 

To try and alleviate having each player have live searches on the private leagues own trade site on poe-trade active at all times of playing, I figured a combination of API-monitoring and a Discord combo could solve this.

The idea is, that you get the bot for your private league's Discord and use it in a channel, where each player can make a list of items they want for their build. Then when a player in the private league puts that item into a public stash tab, the bot will see it, and notify the player looking for it, probably via a DM on Discord or in a designated text-channel. 

I figured I might as well make it publicly available, in case anyone else was looking for something similar. 

## Implemented features

Making and maintaining a wishlist by adding and removing items to and from it
Seeing your current wishlist

## Current goals

Poll the PoE public stash API every 10 minutes for new uniques in stash tabs

Match incoming items against a user-defined wish list

Notify via a dedicated Discord channel or DMs

## Future goals (see TODO file as well for this)

Other items (Transfigured gems, specific (influenced) item bases etc.)  
Total overview over found uniques in a private league

## Buildstack

Language: Java (JDA) – chosen because it’s my most proficient from university

API: REST calls to the PoE public stash API

Database: SQLite via JDBC for storing wishlists and alerts

Containerisation: Docker for easy deployment

# Disclaimer:

This product isn't affiliated with or endorsed by Grinding Gear Games in any way.