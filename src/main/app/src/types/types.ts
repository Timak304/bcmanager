export type Tag = {
    id: number;
    name: string;
};

export type TagCount = {
    name: string;
    count: number;
};

export type Album = {
    id: number;
    artist: string;
    name: string;
    url: string;
    location: string;
    releaseDate: string;
    trackList: string;
    duration: number;
    category: "COLLECTION" | "WISHLIST"
    tagList: string[];
};

export type ImportFanPageStatus = {
    total: number;
    current: number;
    errors: string;
};