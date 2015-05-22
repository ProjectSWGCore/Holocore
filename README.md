### **Branch** ###

**Development:** Upload your Pull Requests here so it can be tested

**Master:** Tested and approved commits will be moved to this branch (Test Center)

**Release:** This is the live version of latest code run on Live Server (Tydirium)

--------------------------------------------------------------------------------

![holocore.png](https://bitbucket.org/repo/norXdj/images/3473411954-holocore.png)

## Copyright (c) 2015 /// Project SWG /// www.projectswg.com ##

ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on
July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.
Our goal is to create an emulator which will provide a server for players to
continue playing a game similar to the one they used to play. We are basing
it on the final publish of the game prior to end-game events.

--------------------------------------------------------------------------------

Holocore is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

Holocore is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with Holocore.  If not, see <http://www.gnu.org/licenses/>.

### Running Holocore ###
In order to successfully build and run Holocore, you must:

1. Have a valid Star Wars Galaxies installation that is updated from the final patch.
2. Setup a postgresql server, preferably on localhost, with a database and user named nge with read/write permissions.
    * Note: You will need to restore the database that you created using nge.backup in order to create the proper tables.
3. Extract the following contents of the sku's to a new clientdata folder in the holocore directory:
    * abstract
    * appearance
    * creation
    * customization 
    * datatables 
    * footprint
    * interiorlayout
    * misc
    * object
    * quest
    * snapshot
    * string
    * terrain